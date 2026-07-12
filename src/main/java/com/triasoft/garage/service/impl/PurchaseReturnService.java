package com.triasoft.garage.service.impl;

import com.triasoft.garage.concurrency.VersionCheck;

import com.triasoft.garage.constants.*;
import com.triasoft.garage.dto.PurchaseReturnDTO;
import com.triasoft.garage.dto.PurchaseReturnReceiptDTO;
import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.entity.*;
import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.helper.LookupHelper;
import com.triasoft.garage.model.purchase.*;
import com.triasoft.garage.model.report.PurchaseReturnReceivableInfo;
import com.triasoft.garage.model.report.PurchaseReturnReceivablesSummaryRs;
import com.triasoft.garage.projection.PurchaseReturnReceivableRow;
import com.triasoft.garage.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PurchaseReturnService {

    private final InventoryRepository inventoryRepository;
    private final PaymentAccountRepository paymentAccountRepository;
    private final TransactionRepository transactionRepository;
    private final PurchaseReturnRepository purchaseReturnRepository;
    private final PurchaseReturnReceiptRepository purchaseReturnReceiptRepository;
    private final PurchasePaymentRepository purchasePaymentRepository;
    private final ExpenseRepository expenseRepository;
    private final JournalService journalService;
    private final LookupHelper lookupHelper;

    // ─────────────────────────────────────────────────────────────────────────
    //  Form data — pre-fills the return form
    // ─────────────────────────────────────────────────────────────────────────

    public PurchaseReturnFormDataRs getFormData(Long inventoryId, UserDTO user) {
        Inventory inv = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new EntityNotFoundException("Inventory not found"));
        if (inv.getSourceSaleId() != null) {
            throw new BusinessException(ErrorCode.Business.CANNOT_RETURN_EXCHANGE_INVENTORY_DIRECTLY);
        }
        if (purchaseReturnRepository.existsByInventoryId(inventoryId)) {
            throw new BusinessException(ErrorCode.Business.PURCHASE_RETURN_ALREADY_EXISTS);
        }
        if (StatusEnum.SOLD.equals(inv.getStatus())) {
            throw new BusinessException(ErrorCode.Business.PURCHASE_RETURN_INVENTORY_SOLD);
        }
        Purchase purchase = inv.getPurchaseOrderDetail().getPurchase();
        BigDecimal vendorInvoice = computeVendorInvoiceAmount(purchase);
        BigDecimal paidToVendor = purchasePaymentRepository.sumAmountByPurchaseId(purchase.getId());
        BigDecimal outstandingAp = vendorInvoice.subtract(paidToVendor).max(BigDecimal.ZERO);

        return PurchaseReturnFormDataRs.builder()
                .inventoryId(inv.getId())
                .uin(inv.getUin())
                .vehicleNo(inv.getProductNo())
                .purchaseId(purchase.getId())
                .purchaseReferenceNo(purchase.getReferenceNo())
                .vendorName(purchase.getVendor() != null ? purchase.getVendor().getName() : null)
                .purchaseDate(purchase.getOrderDate())
                .inventoryLandedCost(inv.getLandedCost())
                .vendorInvoiceAmount(vendorInvoice)
                .paidToVendor(paidToVendor)
                .outstandingAp(outstandingAp)
                .suggestedRefundAmount(paidToVendor)
                .maxRefundAmount(paidToVendor)
                .build();
    }

    private BigDecimal computeVendorInvoiceAmount(Purchase purchase) {
        BigDecimal expensesSum = expenseRepository.findByPurchaseId(purchase.getId()).stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return purchase.getTotalAmount().subtract(expensesSum).max(BigDecimal.ZERO);
    }

    /** unwindAmount = outstandingAp + refundAmount; this is what the journal DRs against A/P. */
    private BigDecimal computeUnwindAmount(BigDecimal refundAmount, Purchase purchase) {
        BigDecimal outstandingAp = computeOutstandingAp(purchase);
        return outstandingAp.add(refundAmount);
    }

    /** Reverse of unwind: refundAmount = unwindAmount(stored) - outstandingAp. */
    private BigDecimal computeRefundFromStored(BigDecimal unwindAmount, Purchase purchase) {
        BigDecimal outstandingAp = computeOutstandingAp(purchase);
        return unwindAmount.subtract(outstandingAp).max(BigDecimal.ZERO);
    }

    private BigDecimal computeOutstandingAp(Purchase purchase) {
        BigDecimal vendorInvoice = computeVendorInvoiceAmount(purchase);
        BigDecimal paidToVendor = purchasePaymentRepository.sumAmountByPurchaseId(purchase.getId());
        return vendorInvoice.subtract(paidToVendor).max(BigDecimal.ZERO);
    }


    @Transactional
    public PurchaseReturnDTO create(Long inventoryId, PurchaseReturnRq rq, UserDTO user) {
        Inventory inv = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new EntityNotFoundException("Inventory not found"));
        if (inv.getSourceSaleId() != null) {
            throw new BusinessException(ErrorCode.Business.CANNOT_RETURN_EXCHANGE_INVENTORY_DIRECTLY);
        }
        if (purchaseReturnRepository.existsByInventoryId(inventoryId)) {
            throw new BusinessException(ErrorCode.Business.PURCHASE_RETURN_ALREADY_EXISTS);
        }
        if (StatusEnum.SOLD.equals(inv.getStatus())) {
            throw new BusinessException(ErrorCode.Business.PURCHASE_RETURN_INVENTORY_SOLD);
        }
        if (!StatusEnum.AVAILABLE.equals(inv.getStatus()) && !StatusEnum.PENDING_DELIVERY.equals(inv.getStatus())) {
            throw new BusinessException(ErrorCode.Business.PURCHASE_RETURN_INVENTORY_UNAVAILABLE);
        }

        Purchase purchase = inv.getPurchaseOrderDetail().getPurchase();

        BigDecimal paidToVendor = purchasePaymentRepository.sumAmountByPurchaseId(purchase.getId());
        // Default refund = paidToVendor (vendor refunds everything we paid them).
        BigDecimal refundAmount = rq.getRefundAmount() != null ? rq.getRefundAmount() : paidToVendor;
        if (refundAmount.signum() < 0) {
            throw new BusinessException(ErrorCode.Business.INVALID_RETURN_AMOUNT);
        }
        // Cap: vendor never refunds more than we paid them.
        if (refundAmount.compareTo(paidToVendor) > 0) {
            throw new BusinessException(ErrorCode.Business.REFUND_EXCEEDS_PAID_TO_VENDOR);
        }

        BigDecimal unwindAmount = computeUnwindAmount(refundAmount, purchase);

        PurchaseReturn pr = new PurchaseReturn();
        pr.setPurchase(purchase);
        pr.setInventory(inv);
        pr.setReturnDate(rq.getReturnDate() != null ? rq.getReturnDate() : LocalDate.now());
        pr.setReason(rq.getReason());
        pr.setNotes(rq.getNotes());
        pr.setInventoryLandedCost(inv.getLandedCost());
        // Column `return_amount` semantically holds the unwind value used by the journal.
        pr.setReturnAmount(unwindAmount);
        // If vendor isn't refunding anything (full restocking), mark COMPLETED immediately.
        pr.setStatus(refundAmount.signum() <= 0 ? ReturnStatusEnum.COMPLETED : ReturnStatusEnum.PENDING);
        pr = purchaseReturnRepository.save(pr);

        // Mark inventory as returned. Do NOT soft-delete — the PurchaseReturn row holds a FK
        // to this inventory, so removing it from the persistence context here would cause a
        // TransientObjectException on the next auto-flush. Stock listings already filter by
        // status (AVAILABLE / PENDING_DELIVERY), so RETURNED_TO_VENDOR rows stay out of the UI.
        inv.setStatus(StatusEnum.RETURNED_TO_VENDOR);
        inventoryRepository.save(inv);

        // If no inventory items for this purchase have status other than RETURNED_TO_VENDOR,
        // the whole PO is effectively returned.
        if (allInventoryReturned(purchase.getId())) {
            LookupMaster returnedStatus = lookupHelper.getStatus(LookupTypeEnum.PURCHASE_STATUS, StatusEnum.RETURNED);
            if (returnedStatus != null) {
                purchase.setStatus(returnedStatus);
            }
        }

        journalService.post(JournalService.REF_PURCHASE_RETURN, pr.getId());

        BigDecimal loss = inv.getLandedCost().subtract(unwindAmount).max(BigDecimal.ZERO);
        return PurchaseReturnDTO.builder()
                .id(pr.getId())
                .purchaseId(purchase.getId())
                .inventoryId(inv.getId())
                .refundAmount(refundAmount)
                .unwindAmount(unwindAmount)
                .lossOnReturn(loss)
                .status(pr.getStatus())
                .build();
    }

    private boolean allInventoryReturned(Long purchaseId) {
        // True if there is no inventory item under this purchase with a status other than RETURNED_TO_VENDOR.
        return !inventoryRepository.existsByPurchaseOrderDetailPurchaseIdAndStatusNot(
                purchaseId, StatusEnum.RETURNED_TO_VENDOR);
    }

    public PurchaseReturnDTO get(Long id, UserDTO user) {
        PurchaseReturn pr = purchaseReturnRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.PURCHASE_RETURN_NOT_FOUND));
        return toPurchaseReturnDTO(pr);
    }

    public PurchaseReturnReceivablesSummaryRs getReceivablesSummary() {
        List<PurchaseReturnReceivableRow> rows = purchaseReturnRepository.findReceivables();
        List<PurchaseReturnReceivableInfo> items = rows.stream().map(r -> PurchaseReturnReceivableInfo.builder()
                .purchaseReturnId(r.getPurchaseReturnId())
                .purchaseId(r.getPurchaseId())
                .purchaseReferenceNo(r.getPurchaseReferenceNo())
                .vehicleNo(r.getVehicleNo())
                .returnDate(r.getReturnDate())
                .cashRefundExpected(safe(r.getCashRefundExpected()))
                .pendingAmount(safe(r.getPendingAmount()))
                .lastReceiptDate(r.getLastReceiptDate())
                .vendorName(r.getVendorName())
                .vendorMobile(r.getVendorMobile())
                .build()).toList();
        BigDecimal totalPending = items.stream()
                .map(PurchaseReturnReceivableInfo::getPendingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return PurchaseReturnReceivablesSummaryRs.builder()
                .totalCount(items.size())
                .totalPendingAmount(totalPending)
                .items(items)
                .build();
    }

    private BigDecimal safe(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    public PurchaseReturnRs list(LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        Page<PurchaseReturn> page = purchaseReturnRepository.findByDateRange(fromDate, toDate, pageable);
        List<PurchaseReturnDTO> returns = page.getContent().stream().map(this::toPurchaseReturnDTO).toList();
        PurchaseReturnRs rs = PurchaseReturnRs.builder().purchasesReturns(returns).build();
        rs.setTotalPages(page.getTotalPages());
        rs.setTotalElements(page.getTotalElements());
        return rs;
    }

    private PurchaseReturnDTO toPurchaseReturnDTO(PurchaseReturn pr) {
        BigDecimal totalReceived = purchaseReturnReceiptRepository.sumAmountByPurchaseReturnId(pr.getId());
        BigDecimal vendorInvoice = computeVendorInvoiceAmount(pr.getPurchase());
        BigDecimal paidToVendor = purchasePaymentRepository.sumAmountByPurchaseId(pr.getPurchase().getId());
        BigDecimal outstandingAp = vendorInvoice.subtract(paidToVendor).max(BigDecimal.ZERO);
        BigDecimal unwindAmount = pr.getReturnAmount();
        BigDecimal refundAmount = computeRefundFromStored(unwindAmount, pr.getPurchase());
        BigDecimal remaining = refundAmount.subtract(totalReceived).max(BigDecimal.ZERO);
        BigDecimal loss = pr.getInventoryLandedCost().subtract(unwindAmount).max(BigDecimal.ZERO);

        List<PurchaseReturnReceiptDTO> receipts = purchaseReturnReceiptRepository
                .findByPurchaseReturnIdOrderByPaymentDateDesc(pr.getId())
                .stream().map(this::toReceiptDTO).toList();

        Inventory inv = pr.getInventory();
        return PurchaseReturnDTO.builder()
                .id(pr.getId())
                .purchaseId(pr.getPurchase().getId())
                .purchaseReferenceNo(pr.getPurchase().getReferenceNo())
                .inventoryId(inv.getId())
                .uin(inv.getUin())
                .vehicleNo(inv.getProductNo())
                .vendorName(pr.getPurchase().getVendor() != null ? pr.getPurchase().getVendor().getName() : null)
                .returnDate(pr.getReturnDate())
                .reason(pr.getReason())
                .notes(pr.getNotes())
                .inventoryLandedCost(pr.getInventoryLandedCost())
                .vendorInvoiceAmount(vendorInvoice)
                .paidToVendor(paidToVendor)
                .outstandingAp(outstandingAp)
                .refundAmount(refundAmount)
                .unwindAmount(unwindAmount)
                .lossOnReturn(loss)
                .status(pr.getStatus())
                .totalReceived(totalReceived)
                .remainingReceivable(remaining)
                .receipts(receipts)
                .build();
    }

    private PurchaseReturnReceiptDTO toReceiptDTO(PurchaseReturnReceipt r) {
        return PurchaseReturnReceiptDTO.builder()
                .id(r.getId())
                .version(r.getVersion())
                .amount(r.getAmount())
                .paymentDate(r.getPaymentDate())
                .paymentMethod(r.getPaymentMethod())
                .paymentAccountId(r.getPaymentAccount() != null ? r.getPaymentAccount().getId() : null)
                .paymentAccountName(r.getPaymentAccount() != null ? r.getPaymentAccount().getName() : null)
                .referenceNo(r.getReferenceNo())
                .notes(r.getNotes())
                .build();
    }


    @Transactional
    public ReceiptCreateRs recordReceipt(Long returnId, PurchaseReturnReceiptRq rq, UserDTO user) {
        PurchaseReturn pr = purchaseReturnRepository.findById(returnId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.PURCHASE_RETURN_NOT_FOUND));
        BigDecimal refundAmount = computeRefundFromStored(pr.getReturnAmount(), pr.getPurchase());
        BigDecimal alreadyReceived = purchaseReturnReceiptRepository.sumAmountByPurchaseReturnId(returnId);
        BigDecimal remaining = refundAmount.subtract(alreadyReceived);
        if (remaining.signum() <= 0) {
            throw new BusinessException(ErrorCode.Business.RECEIPT_EXCEEDS_REMAINING);
        }
        if (rq.getAmount() == null || rq.getAmount().signum() <= 0) {
            throw new BusinessException(ErrorCode.Business.INVALID_RETURN_AMOUNT);
        }
        if (rq.getAmount().compareTo(remaining) > 0) {
            throw new BusinessException(ErrorCode.Business.RECEIPT_EXCEEDS_REMAINING);
        }
        PaymentAccount account = resolveAccount(rq.getPaymentAccountId());

        PurchaseReturnReceipt receipt = new PurchaseReturnReceipt();
        receipt.setPurchaseReturn(pr);
        receipt.setAmount(rq.getAmount());
        receipt.setPaymentDate(rq.getPaymentDate() != null ? rq.getPaymentDate() : LocalDate.now());
        receipt.setPaymentMethod(rq.getPaymentMethod());
        receipt.setReferenceNo(rq.getReferenceNo());
        receipt.setNotes(rq.getNotes());
        receipt.setPaymentAccount(account);
        PurchaseReturnReceipt saved = purchaseReturnReceiptRepository.save(receipt);

        createReceiptTransaction(saved, pr.getPurchase().getReferenceNo());
        BigDecimal newTotal = alreadyReceived.add(rq.getAmount());
        if (newTotal.compareTo(refundAmount) >= 0) {
            pr.setStatus(ReturnStatusEnum.COMPLETED);
            purchaseReturnRepository.save(pr);
        }
        return ReceiptCreateRs.builder()
                .receiptId(saved.getId())
                .purchaseReturnId(pr.getId())
                .amount(saved.getAmount())
                .totalReceived(newTotal)
                .remainingReceivable(refundAmount.subtract(newTotal).max(BigDecimal.ZERO))
                .status(pr.getStatus())
                .build();
    }

    @Transactional
    @VersionCheck(entity = PurchaseReturnReceipt.class, idIndex = 1)
    public ReceiptCreateRs updateReceipt(Long returnId, Long receiptId, PurchaseReturnReceiptRq rq, UserDTO user) {
        PurchaseReturn pr = purchaseReturnRepository.findById(returnId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.PURCHASE_RETURN_NOT_FOUND));
        PurchaseReturnReceipt receipt = purchaseReturnReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.PURCHASE_RETURN_RECEIPT_NOT_FOUND));
        if (!receipt.getPurchaseReturn().getId().equals(returnId)) {
            throw new BusinessException(ErrorCode.Business.PURCHASE_RETURN_RECEIPT_NOT_FOUND);
        }
        boolean amountChanged = receipt.getAmount().compareTo(rq.getAmount()) != 0;
        Long oldAccountId = receipt.getPaymentAccount() != null ? receipt.getPaymentAccount().getId() : null;
        boolean accountChanged = !Objects.equals(oldAccountId, rq.getPaymentAccountId());

        if (amountChanged || accountChanged) {
            BigDecimal refundAmount = computeRefundFromStored(pr.getReturnAmount(), pr.getPurchase());
            BigDecimal alreadyReceived = purchaseReturnReceiptRepository.sumAmountByPurchaseReturnId(returnId);
            BigDecimal excludingThis = alreadyReceived.subtract(receipt.getAmount());
            BigDecimal remaining = refundAmount.subtract(excludingThis);
            if (rq.getAmount().compareTo(remaining) > 0) {
                throw new BusinessException(ErrorCode.Business.RECEIPT_EXCEEDS_REMAINING);
            }
            reverseReceiptTransaction(receipt);
        }

        receipt.setAmount(rq.getAmount());
        receipt.setPaymentDate(rq.getPaymentDate() != null ? rq.getPaymentDate() : receipt.getPaymentDate());
        receipt.setPaymentMethod(rq.getPaymentMethod());
        receipt.setReferenceNo(rq.getReferenceNo());
        receipt.setNotes(rq.getNotes());
        PaymentAccount newAccount = resolveAccount(rq.getPaymentAccountId());
        receipt.setPaymentAccount(newAccount);
        PurchaseReturnReceipt saved = purchaseReturnReceiptRepository.save(receipt);

        if (amountChanged || accountChanged) {
            createReceiptTransaction(saved, pr.getPurchase().getReferenceNo());
        }
        BigDecimal newTotal = purchaseReturnReceiptRepository.sumAmountByPurchaseReturnId(returnId);
        BigDecimal refundAmount = computeRefundFromStored(pr.getReturnAmount(), pr.getPurchase());
        recomputeStatus(pr, newTotal, refundAmount);
        return ReceiptCreateRs.builder()
                .receiptId(saved.getId())
                .purchaseReturnId(pr.getId())
                .amount(saved.getAmount())
                .totalReceived(newTotal)
                .remainingReceivable(refundAmount.subtract(newTotal).max(BigDecimal.ZERO))
                .status(pr.getStatus())
                .build();
    }

    @Transactional
    public void deleteReceipt(Long returnId, Long receiptId, UserDTO user) {
        PurchaseReturn pr = purchaseReturnRepository.findById(returnId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.PURCHASE_RETURN_NOT_FOUND));
        PurchaseReturnReceipt receipt = purchaseReturnReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.PURCHASE_RETURN_RECEIPT_NOT_FOUND));
        if (!receipt.getPurchaseReturn().getId().equals(returnId)) {
            throw new BusinessException(ErrorCode.Business.PURCHASE_RETURN_RECEIPT_NOT_FOUND);
        }
        reverseReceiptTransaction(receipt);
        purchaseReturnReceiptRepository.delete(receipt);
        BigDecimal newTotal = purchaseReturnReceiptRepository.sumAmountByPurchaseReturnId(returnId);
        BigDecimal refundAmount = computeRefundFromStored(pr.getReturnAmount(), pr.getPurchase());
        recomputeStatus(pr, newTotal, refundAmount);
    }

    private void recomputeStatus(PurchaseReturn pr, BigDecimal totalReceived, BigDecimal refundAmount) {
        if (totalReceived.compareTo(refundAmount) >= 0) {
            pr.setStatus(ReturnStatusEnum.COMPLETED);
        } else {
            pr.setStatus(ReturnStatusEnum.PENDING);
        }
        purchaseReturnRepository.save(pr);
    }

    private PaymentAccount resolveAccount(Long accountId) {
        if (accountId == null) {
            throw new BusinessException(ErrorCode.Business.PAYMENT_ACCOUNT_REQUIRED);
        }
        return paymentAccountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.PAYMENT_ACCOUNT_NOT_FOUND));
    }

    private void createReceiptTransaction(PurchaseReturnReceipt receipt, String purchaseRef) {
        Transaction transaction = new Transaction();
        transaction.setTransactionDate(receipt.getPaymentDate());
        transaction.setType(TransactionTypeEnum.REFUND);
        transaction.setReferenceType("PURCHASE_RETURN_RECEIPT");
        transaction.setReferenceId(receipt.getId());
        transaction.setPaymentAccount(receipt.getPaymentAccount());
        transaction.setAmount(receipt.getAmount());
        transaction.setDirection(TransactionDirectionEnum.IN);
        transaction.setDescription("Vendor refund received – " + purchaseRef);
        transaction.setNotes(receipt.getNotes());
        transactionRepository.save(transaction);
        journalService.post(JournalService.REF_PURCHASE_RETURN_RECEIPT, receipt.getId());
    }

    private void reverseReceiptTransaction(PurchaseReturnReceipt receipt) {
        transactionRepository.findActiveByReferenceTypeAndReferenceId("PURCHASE_RETURN_RECEIPT", receipt.getId())
                .ifPresent(original -> {
                    if (transactionRepository.existsByReversalOfId(original.getId())) return;
                    Transaction reversal = new Transaction();
                    reversal.setTransactionDate(LocalDate.now());
                    reversal.setType(TransactionTypeEnum.REFUND);
                    reversal.setReferenceType("PURCHASE_RETURN_RECEIPT");
                    reversal.setReferenceId(receipt.getId());
                    reversal.setPaymentAccount(original.getPaymentAccount());
                    reversal.setAmount(original.getAmount());
                    reversal.setDirection(TransactionDirectionEnum.OUT);
                    reversal.setDescription("Reversal – " + original.getDescription());
                    reversal.setReversalOf(original);
                    transactionRepository.save(reversal);
                    journalService.reverseOnDate(JournalService.REF_PURCHASE_RETURN_RECEIPT, receipt.getId(), LocalDate.now());
                });
    }
}
