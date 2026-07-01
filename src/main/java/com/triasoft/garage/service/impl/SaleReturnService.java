package com.triasoft.garage.service.impl;

import com.triasoft.garage.constants.*;
import com.triasoft.garage.dto.DeductionDTO;
import com.triasoft.garage.dto.RefundPaymentDTO;
import com.triasoft.garage.dto.SaleReturnDTO;
import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.entity.*;
import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.helper.LookupHelper;
import com.triasoft.garage.model.report.SaleReturnPayableInfo;
import com.triasoft.garage.model.report.SaleReturnPayablesSummaryRs;
import com.triasoft.garage.model.sale.*;
import com.triasoft.garage.projection.SaleReturnPayableRow;
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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SaleReturnService {

    private final SaleRepository saleRepository;
    private final SalePaymentRepository salePaymentRepository;
    private final InventoryRepository inventoryRepository;
    private final ExpenseRepository expenseRepository;
    private final PaymentAccountRepository paymentAccountRepository;
    private final TransactionRepository transactionRepository;
    private final SaleReturnRepository saleReturnRepository;
    private final SaleReturnDeductionRepository saleReturnDeductionRepository;
    private final SaleRefundPaymentRepository saleRefundPaymentRepository;
    private final JournalService journalService;
    private final PurchaseService purchaseService;
    private final PurchaseRepository purchaseRepository;
    private final LookupHelper lookupHelper;

    // ─────────────────────────────────────────────────────────────────────────
    //  Form data — what the UI needs to populate the return form
    // ─────────────────────────────────────────────────────────────────────────

    public ReturnFormDataRs.Body getFormData(Long saleId, UserDTO user) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new EntityNotFoundException("Sale not found"));
        if (saleReturnRepository.existsBySaleId(saleId)) {
            throw new BusinessException(ErrorCode.Business.SALE_ALREADY_RETURNED);
        }
        BigDecimal customerPaid = salePaymentRepository
                .sumAmountBySaleIdAndPayerType(saleId, PayerTypeEnum.CUSTOMER);

        Inventory soldInv = sale.getInventory();
        Long soldPurchaseId = soldInv.getPurchaseOrderDetail().getPurchase().getId();
        List<ReturnFormDataRs.ExpenseItem> soldExpenses = expenseRepository.findByPurchaseId(soldPurchaseId)
                .stream().map(this::toExpenseItem).toList();

        ReturnFormDataRs.VehicleInfo soldVehicleInfo = ReturnFormDataRs.VehicleInfo.builder()
                .inventoryId(soldInv.getId())
                .uin(soldInv.getUin())
                .vehicleNo(soldInv.getProductNo())
                .landedCost(soldInv.getLandedCost())
                .expenses(soldExpenses)
                .build();

        ReturnFormDataRs.ExchangeInfo exchangeInfo = null;
        if (sale.isExchanged()) {
            Optional<Inventory> exchInvOpt = inventoryRepository.findBySourceSaleId(saleId);
            if (exchInvOpt.isPresent()) {
                Inventory exchInv = exchInvOpt.get();
                Long exchPurchaseId = exchInv.getPurchaseOrderDetail().getPurchase().getId();
                List<ReturnFormDataRs.ExpenseItem> exchExpenses = expenseRepository.findByPurchaseId(exchPurchaseId)
                        .stream().map(this::toExpenseItem).toList();
                exchangeInfo = ReturnFormDataRs.ExchangeInfo.builder()
                        .purchaseId(exchPurchaseId)
                        .inventoryId(exchInv.getId())
                        .uin(exchInv.getUin())
                        .vehicleNo(exchInv.getProductNo())
                        .originalExchangeAmount(sale.getExchangeAmount())
                        .currentLandedCost(exchInv.getLandedCost())
                        .expenses(exchExpenses)
                        .build();
            }
        }
        return ReturnFormDataRs.Body.builder()
                .saleId(sale.getId())
                .invoiceNo(sale.getInvoiceNo())
                .saleDate(sale.getSaleDate())
                .saleRate(sale.getSaleRate())
                .customerPaidAmount(customerPaid)
                .isFinanced(sale.isFinanced())
                .isExchanged(sale.isExchanged())
                .soldVehicle(soldVehicleInfo)
                .exchangeVehicle(exchangeInfo)
                .build();
    }

    private ReturnFormDataRs.ExpenseItem toExpenseItem(Expense e) {
        String desc = e.getDescription() != null ? e.getDescription()
                : (e.getExpenseAccount() != null ? e.getExpenseAccount().getLabel() : "Expense");
        return ReturnFormDataRs.ExpenseItem.builder()
                .expenseId(e.getId())
                .description(desc)
                .amount(e.getAmount())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Create the sale return
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public SaleReturnDTO create(Long saleId, SaleReturnRq rq, UserDTO user) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new EntityNotFoundException("Sale not found"));

        if (saleReturnRepository.existsBySaleId(saleId)) {
            throw new BusinessException(ErrorCode.Business.SALE_ALREADY_RETURNED);
        }
        if (sale.isFinanced()) {
            throw new BusinessException(ErrorCode.Business.SALE_RETURN_FINANCED_NOT_ALLOWED);
        }

        ExchangeHandlingEnum handling = resolveExchangeHandling(rq.getExchangeHandling(), sale);
        BigDecimal customerPaid = salePaymentRepository
                .sumAmountBySaleIdAndPayerType(saleId, PayerTypeEnum.CUSTOMER);

        BigDecimal soldDed = sumAmounts(rq.getSoldVehicleDeductions());
        BigDecimal exchDed = sumAmounts(rq.getExchangeVehicleDeductions());

        validateBuyback(handling, rq.getExchangeBuybackAmount(), sale);
        validateExchangeDeductions(handling, rq.getExchangeVehicleDeductions());
        validateDeductionCaps(handling, sale, customerPaid, soldDed, exchDed, rq.getExchangeBuybackAmount());

        BigDecimal refundAmount = computeRefundAmount(handling, customerPaid,
                rq.getExchangeBuybackAmount(), soldDed, exchDed);

        SaleReturn sr = new SaleReturn();
        sr.setSale(sale);
        sr.setReturnDate(rq.getReturnDate() != null ? rq.getReturnDate() : LocalDate.now());
        sr.setReason(rq.getReason());
        sr.setNotes(rq.getNotes());
        sr.setCustomerPaidAmount(customerPaid);
        sr.setExchangeHandling(handling);
        sr.setExchangeBuybackAmount(handling == ExchangeHandlingEnum.KEEP_AND_BUYBACK
                ? rq.getExchangeBuybackAmount() : null);
        sr.setSoldVehicleDeductionAmount(soldDed);
        sr.setExchangeVehicleDeductionAmount(exchDed);
        sr.setRefundAmount(refundAmount);
        sr.setStatus(refundAmount.signum() <= 0 ? ReturnStatusEnum.COMPLETED : ReturnStatusEnum.PENDING);
        sr = saleReturnRepository.save(sr);

        saveDeductions(sr, rq.getSoldVehicleDeductions(), DeductionContextEnum.SOLD);
        saveDeductions(sr, rq.getExchangeVehicleDeductions(), DeductionContextEnum.EXCHANGE);

        // Sold vehicle returns to available stock
        Inventory soldInv = sale.getInventory();
        soldInv.setStatus(StatusEnum.AVAILABLE);
        inventoryRepository.save(soldInv);

        // Exchange vehicle: only reverse if returned to buyer
        if (handling == ExchangeHandlingEnum.RETURN_TO_BUYER) {
            Inventory exchInv = inventoryRepository.findBySourceSaleId(saleId)
                    .orElseThrow(() -> new EntityNotFoundException("Exchange inventory not found"));
            if (StatusEnum.SOLD.equals(exchInv.getStatus())) {
                throw new BusinessException(ErrorCode.Business.EXCHANGE_VEHICLE_ALREADY_SOLD);
            }
            Long exchPurchaseId = exchInv.getPurchaseOrderDetail().getPurchase().getId();
            // Reverse all PURCHASE-side journals/transactions for the trade-in (expenses, payments, purchase),
            // soft-delete it, remove inventory.
            purchaseService.delete(exchPurchaseId, user);
        }

        // Sale status → RETURNED, payment status → REFUND
        LookupMaster returnedStatus = lookupHelper.getStatus(LookupTypeEnum.SALES_STAUS, StatusEnum.RETURNED);
        if (returnedStatus != null) {
            sale.setStatus(returnedStatus);
        }
        sale.setPaymentStatus(StatusEnum.REFUND);
        saleRepository.save(sale);

        journalService.post(JournalService.REF_SALE_RETURN, sr.getId());

        // For KEEP_AND_BUYBACK: post a PURCHASE journal for the exchange vehicle and
        // update its cost basis to the buyback amount (was the original exchange amount).
        if (handling == ExchangeHandlingEnum.KEEP_AND_BUYBACK && sale.isExchanged()) {
            final BigDecimal buyback = sr.getExchangeBuybackAmount() != null
                    ? sr.getExchangeBuybackAmount() : BigDecimal.ZERO;
            final BigDecimal exchangeAmount = safeAmt(sale.getExchangeAmount());
            final LocalDate returnDate = sr.getReturnDate();
            final String customerName = sale.getCustomer().getName();
            inventoryRepository.findBySourceSaleId(saleId).ifPresent(exchInv -> {
                Purchase exchPurchase = exchInv.getPurchaseOrderDetail().getPurchase();
                BigDecimal delta = buyback.subtract(exchangeAmount);
                if (delta.compareTo(BigDecimal.ZERO) != 0) {
                    exchPurchase.setTotalAmount(exchPurchase.getTotalAmount().add(delta));
                    exchInv.setLandedCost(exchInv.getLandedCost().add(delta));
                    inventoryRepository.save(exchInv);
                }
                // Mark the purchase as a buyback-recorded standalone purchase. From this
                // point on, update/delete/findPayables treat it as a regular purchase,
                // not as an unsettled exchange.
                exchPurchase.setBuybackRecordedAt(returnDate);
                purchaseRepository.save(exchPurchase);
                journalService.postExchangeBuybackPurchase(
                        exchPurchase.getId(), buyback, returnDate, customerName);
            });
        }

        return SaleReturnDTO.builder()
                .id(sr.getId())
                .saleId(sale.getId())
                .refundAmount(refundAmount)
                .status(sr.getStatus())
                .build();
    }

    private ExchangeHandlingEnum resolveExchangeHandling(ExchangeHandlingEnum requested, Sale sale) {
        if (!sale.isExchanged()) {
            if (requested != null && requested != ExchangeHandlingEnum.NONE) {
                throw new BusinessException(ErrorCode.Business.EXCHANGE_HANDLING_INVALID);
            }
            return ExchangeHandlingEnum.NONE;
        }
        if (requested == null || requested == ExchangeHandlingEnum.NONE) {
            throw new BusinessException(ErrorCode.Business.EXCHANGE_HANDLING_INVALID);
        }
        return requested;
    }

    private void validateBuyback(ExchangeHandlingEnum handling, BigDecimal buyback, Sale sale) {
        if (handling == ExchangeHandlingEnum.KEEP_AND_BUYBACK) {
            if (buyback == null || buyback.signum() < 0) {
                throw new BusinessException(ErrorCode.Business.EXCHANGE_BUYBACK_AMOUNT_INVALID);
            }
            BigDecimal original = sale.getExchangeAmount() != null ? sale.getExchangeAmount() : BigDecimal.ZERO;
            if (buyback.compareTo(original) > 0) {
                throw new BusinessException(ErrorCode.Business.EXCHANGE_BUYBACK_EXCEEDS_ORIGINAL);
            }
        } else if (buyback != null && buyback.signum() != 0) {
            throw new BusinessException(ErrorCode.Business.EXCHANGE_BUYBACK_AMOUNT_INVALID);
        }
    }

    private void validateExchangeDeductions(ExchangeHandlingEnum handling, List<ReturnDeductionRq> exchDeductions) {
        if (handling == ExchangeHandlingEnum.KEEP_AND_BUYBACK) return;
        if (exchDeductions != null && !exchDeductions.isEmpty()) {
            throw new BusinessException(ErrorCode.Business.EXCHANGE_DEDUCTION_NOT_ALLOWED);
        }
    }

    private void validateDeductionCaps(ExchangeHandlingEnum handling, Sale sale, BigDecimal customerPaid,
                                       BigDecimal soldDed, BigDecimal exchDed, BigDecimal buyback) {
        if (soldDed.compareTo(customerPaid) > 0) {
            throw new BusinessException(ErrorCode.Business.DEDUCTION_EXCEEDS_BASE);
        }
        if (handling == ExchangeHandlingEnum.KEEP_AND_BUYBACK) {
            BigDecimal buybackSafe = buyback != null ? buyback : BigDecimal.ZERO;
            if (exchDed.compareTo(buybackSafe) > 0) {
                throw new BusinessException(ErrorCode.Business.DEDUCTION_EXCEEDS_BASE);
            }
        }
    }

    private BigDecimal computeRefundAmount(ExchangeHandlingEnum handling, BigDecimal customerPaid,
                                           BigDecimal buyback, BigDecimal soldDed, BigDecimal exchDed) {
        BigDecimal refund = customerPaid.subtract(soldDed);
        if (handling == ExchangeHandlingEnum.KEEP_AND_BUYBACK) {
            BigDecimal buybackSafe = buyback != null ? buyback : BigDecimal.ZERO;
            refund = refund.add(buybackSafe).subtract(exchDed);
        }
        if (refund.signum() < 0) {
            throw new BusinessException(ErrorCode.Business.INVALID_REFUND_AMOUNT);
        }
        return refund;
    }

    private BigDecimal sumAmounts(List<ReturnDeductionRq> items) {
        if (items == null || items.isEmpty()) return BigDecimal.ZERO;
        return items.stream()
                .map(d -> d.getAmount() != null ? d.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void saveDeductions(SaleReturn sr, List<ReturnDeductionRq> items, DeductionContextEnum ctx) {
        if (items == null) return;
        for (ReturnDeductionRq item : items) {
            if (item.getAmount() == null || item.getAmount().signum() == 0) continue;
            SaleReturnDeduction d = new SaleReturnDeduction();
            d.setSaleReturn(sr);
            d.setVehicleContext(ctx);
            d.setExpenseId(item.getExpenseId());
            d.setDescription(item.getDescription() != null ? item.getDescription() : "Deduction");
            d.setAmount(item.getAmount());
            saleReturnDeductionRepository.save(d);
        }
    }

    public SaleReturnDTO get(Long id, UserDTO user) {
        SaleReturn sr = saleReturnRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.SALE_RETURN_NOT_FOUND));
        return toSaleReturnDTO(sr);
    }

    public SaleReturnPayablesSummaryRs getPayablesSummary() {
        List<SaleReturnPayableRow> rows = saleReturnRepository.findPayables();
        List<SaleReturnPayableInfo> items = rows.stream().map(r -> SaleReturnPayableInfo.builder()
                .saleReturnId(r.getSaleReturnId())
                .saleId(r.getSaleId())
                .invoiceNo(r.getInvoiceNo())
                .vehicleNo(r.getVehicleNo())
                .returnDate(r.getReturnDate())
                .refundAmount(safeAmt(r.getRefundAmount()))
                .pendingAmount(safeAmt(r.getPendingAmount()))
                .lastRefundDate(r.getLastRefundDate())
                .customerName(r.getCustomerName())
                .customerMobile(r.getCustomerMobile())
                .build()).toList();
        BigDecimal totalPending = items.stream()
                .map(SaleReturnPayableInfo::getPendingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return SaleReturnPayablesSummaryRs.builder()
                .totalCount(items.size())
                .totalPendingAmount(totalPending)
                .items(items)
                .build();
    }

    private BigDecimal safeAmt(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    public SaleReturnRs list(LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        Page<SaleReturn> page = saleReturnRepository.findByDateRange(fromDate, toDate, pageable);
        List<SaleReturnDTO> returns = page.getContent().stream().map(this::toSaleReturnDTO).toList();
        SaleReturnRs rs = SaleReturnRs.builder().saleReturns(returns).build();
        rs.setTotalPages(page.getTotalPages());
        rs.setTotalElements(page.getTotalElements());
        return rs;
    }

    private SaleReturnDTO toSaleReturnDTO(SaleReturn sr) {
        BigDecimal totalRefunded = saleRefundPaymentRepository.sumAmountBySaleReturnId(sr.getId());
        BigDecimal remaining = sr.getRefundAmount().subtract(totalRefunded);
        if (remaining.signum() < 0) remaining = BigDecimal.ZERO;

        List<DeductionDTO> deductions = saleReturnDeductionRepository.findBySaleReturnId(sr.getId())
                .stream().map(this::toDeductionDTO).toList();
        List<RefundPaymentDTO> refunds = saleRefundPaymentRepository
                .findBySaleReturnIdOrderByPaymentDateDesc(sr.getId())
                .stream().map(this::toRefundDTO).toList();

        return SaleReturnDTO.builder()
                .id(sr.getId())
                .saleId(sr.getSale().getId())
                .invoiceNo(sr.getSale().getInvoiceNo())
                .returnDate(sr.getReturnDate())
                .reason(sr.getReason())
                .notes(sr.getNotes())
                .customerPaidAmount(sr.getCustomerPaidAmount())
                .exchangeHandling(sr.getExchangeHandling())
                .exchangeBuybackAmount(sr.getExchangeBuybackAmount())
                .soldVehicleDeductionAmount(sr.getSoldVehicleDeductionAmount())
                .exchangeVehicleDeductionAmount(sr.getExchangeVehicleDeductionAmount())
                .refundAmount(sr.getRefundAmount())
                .status(sr.getStatus())
                .totalRefunded(totalRefunded)
                .remainingRefund(remaining)
                .deductions(deductions)
                .refunds(refunds)
                .build();
    }

    private DeductionDTO toDeductionDTO(SaleReturnDeduction d) {
        return DeductionDTO.builder()
                .id(d.getId())
                .vehicleContext(d.getVehicleContext())
                .expenseId(d.getExpenseId())
                .description(d.getDescription())
                .amount(d.getAmount())
                .build();
    }

    private RefundPaymentDTO toRefundDTO(SaleRefundPayment r) {
        return RefundPaymentDTO.builder()
                .id(r.getId())
                .amount(r.getAmount())
                .paymentDate(r.getPaymentDate())
                .paymentMethod(r.getPaymentMethod())
                .paymentAccountId(r.getPaymentAccount() != null ? r.getPaymentAccount().getId() : null)
                .paymentAccountName(r.getPaymentAccount() != null ? r.getPaymentAccount().getName() : null)
                .referenceNo(r.getReferenceNo())
                .notes(r.getNotes())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Refund payments
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public RefundCreateResponse recordRefund(Long returnId, RefundPaymentRq rq, UserDTO user) {
        SaleReturn sr = saleReturnRepository.findById(returnId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.SALE_RETURN_NOT_FOUND));
        BigDecimal alreadyRefunded = saleRefundPaymentRepository.sumAmountBySaleReturnId(returnId);
        BigDecimal remaining = sr.getRefundAmount().subtract(alreadyRefunded);
        if (remaining.signum() <= 0) {
            throw new BusinessException(ErrorCode.Business.REFUND_EXCEEDS_REMAINING);
        }
        if (rq.getAmount() == null || rq.getAmount().signum() <= 0) {
            throw new BusinessException(ErrorCode.Business.INVALID_REFUND_AMOUNT);
        }
        if (rq.getAmount().compareTo(remaining) > 0) {
            throw new BusinessException(ErrorCode.Business.REFUND_EXCEEDS_REMAINING);
        }
        PaymentAccount account = resolveAndValidateAccount(rq.getPaymentAccountId(), rq.getAmount());

        SaleRefundPayment refund = new SaleRefundPayment();
        refund.setSaleReturn(sr);
        refund.setAmount(rq.getAmount());
        refund.setPaymentDate(rq.getPaymentDate() != null ? rq.getPaymentDate() : LocalDate.now());
        refund.setPaymentMethod(rq.getPaymentMethod());
        refund.setReferenceNo(rq.getReferenceNo());
        refund.setNotes(rq.getNotes());
        refund.setPaymentAccount(account);
        SaleRefundPayment saved = saleRefundPaymentRepository.save(refund);

        createRefundTransaction(saved, sr.getSale().getInvoiceNo());
        BigDecimal newTotal = alreadyRefunded.add(rq.getAmount());
        if (newTotal.compareTo(sr.getRefundAmount()) >= 0) {
            sr.setStatus(ReturnStatusEnum.COMPLETED);
            saleReturnRepository.save(sr);
        }
        return RefundCreateResponse.builder()
                .refundPaymentId(saved.getId())
                .saleReturnId(sr.getId())
                .amount(saved.getAmount())
                .paymentMethod(saved.getPaymentMethod())
                .totalRefunded(newTotal)
                .remainingRefund(sr.getRefundAmount().subtract(newTotal).max(BigDecimal.ZERO))
                .saleReturnStatus(sr.getStatus())
                .build();
    }

    @Transactional
    public RefundCreateResponse updateRefund(Long returnId, Long refundId, RefundPaymentRq rq, UserDTO user) {
        SaleReturn sr = saleReturnRepository.findById(returnId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.SALE_RETURN_NOT_FOUND));
        SaleRefundPayment refund = saleRefundPaymentRepository.findById(refundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.REFUND_PAYMENT_NOT_FOUND));
        if (!refund.getSaleReturn().getId().equals(returnId)) {
            throw new BusinessException(ErrorCode.Business.REFUND_PAYMENT_NOT_FOUND);
        }

        boolean amountChanged = refund.getAmount().compareTo(rq.getAmount()) != 0;
        Long oldAccountId = refund.getPaymentAccount() != null ? refund.getPaymentAccount().getId() : null;
        boolean accountChanged = !Objects.equals(oldAccountId, rq.getPaymentAccountId());

        if (amountChanged || accountChanged) {
            BigDecimal alreadyRefunded = saleRefundPaymentRepository.sumAmountBySaleReturnId(returnId);
            BigDecimal excludingThis = alreadyRefunded.subtract(refund.getAmount());
            BigDecimal remaining = sr.getRefundAmount().subtract(excludingThis);
            if (rq.getAmount().compareTo(remaining) > 0) {
                throw new BusinessException(ErrorCode.Business.REFUND_EXCEEDS_REMAINING);
            }
            reverseRefundTransaction(refund);
        }

        refund.setAmount(rq.getAmount());
        refund.setPaymentDate(rq.getPaymentDate() != null ? rq.getPaymentDate() : refund.getPaymentDate());
        refund.setPaymentMethod(rq.getPaymentMethod());
        refund.setReferenceNo(rq.getReferenceNo());
        refund.setNotes(rq.getNotes());
        PaymentAccount newAccount = resolveAndValidateAccount(rq.getPaymentAccountId(), rq.getAmount());
        refund.setPaymentAccount(newAccount);
        SaleRefundPayment saved = saleRefundPaymentRepository.save(refund);
        if (amountChanged || accountChanged) {
            createRefundTransaction(saved, sr.getSale().getInvoiceNo());
        }
        BigDecimal newTotal = saleRefundPaymentRepository.sumAmountBySaleReturnId(returnId);
        recomputeStatus(sr, newTotal);
        return RefundCreateResponse.builder()
                .refundPaymentId(saved.getId())
                .saleReturnId(sr.getId())
                .amount(saved.getAmount())
                .paymentMethod(saved.getPaymentMethod())
                .totalRefunded(newTotal)
                .remainingRefund(sr.getRefundAmount().subtract(newTotal).max(BigDecimal.ZERO))
                .saleReturnStatus(sr.getStatus())
                .build();
    }

    @Transactional
    public void deleteRefund(Long returnId, Long refundId, UserDTO user) {
        SaleReturn sr = saleReturnRepository.findById(returnId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.SALE_RETURN_NOT_FOUND));
        SaleRefundPayment refund = saleRefundPaymentRepository.findById(refundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.REFUND_PAYMENT_NOT_FOUND));
        if (!refund.getSaleReturn().getId().equals(returnId)) {
            throw new BusinessException(ErrorCode.Business.REFUND_PAYMENT_NOT_FOUND);
        }
        reverseRefundTransaction(refund);
        saleRefundPaymentRepository.delete(refund);
        BigDecimal newTotal = saleRefundPaymentRepository.sumAmountBySaleReturnId(returnId);
        recomputeStatus(sr, newTotal);
    }

    private void recomputeStatus(SaleReturn sr, BigDecimal totalRefunded) {
        if (totalRefunded.compareTo(sr.getRefundAmount()) >= 0) {
            sr.setStatus(ReturnStatusEnum.COMPLETED);
        } else {
            sr.setStatus(ReturnStatusEnum.PENDING);
        }
        saleReturnRepository.save(sr);
    }

    private PaymentAccount resolveAndValidateAccount(Long accountId, BigDecimal amount) {
        if (accountId == null) {
            throw new BusinessException(ErrorCode.Business.PAYMENT_ACCOUNT_REQUIRED);
        }
        PaymentAccount account = paymentAccountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.PAYMENT_ACCOUNT_NOT_FOUND));
        BigDecimal totalIn  = transactionRepository.sumAmountByAccountAndDirection(account.getId(), TransactionDirectionEnum.IN);
        BigDecimal totalOut = transactionRepository.sumAmountByAccountAndDirection(account.getId(), TransactionDirectionEnum.OUT);
        BigDecimal balance  = account.getOpeningBalance().add(totalIn).subtract(totalOut);
        if (balance.compareTo(amount) < 0) {
            throw new BusinessException(ErrorCode.Business.INSUFFICIENT_BALANCE);
        }
        return account;
    }

    private void createRefundTransaction(SaleRefundPayment refund, String invoiceNo) {
        Transaction transaction = new Transaction();
        transaction.setTransactionDate(refund.getPaymentDate());
        transaction.setType(TransactionTypeEnum.REFUND);
        transaction.setReferenceType("SALE_RETURN_REFUND");
        transaction.setReferenceId(refund.getId());
        transaction.setPaymentAccount(refund.getPaymentAccount());
        transaction.setAmount(refund.getAmount());
        transaction.setDirection(TransactionDirectionEnum.OUT);
        transaction.setDescription("Sale return refund – " + invoiceNo);
        transaction.setNotes(refund.getNotes());
        transactionRepository.save(transaction);
        journalService.post(JournalService.REF_SALE_RETURN_REFUND, refund.getId());
    }

    private void reverseRefundTransaction(SaleRefundPayment refund) {
        transactionRepository.findActiveByReferenceTypeAndReferenceId("SALE_RETURN_REFUND", refund.getId())
                .ifPresent(original -> {
                    if (transactionRepository.existsByReversalOfId(original.getId())) return;
                    Transaction reversal = new Transaction();
                    reversal.setTransactionDate(LocalDate.now());
                    reversal.setType(TransactionTypeEnum.REFUND);
                    reversal.setReferenceType("SALE_RETURN_REFUND");
                    reversal.setReferenceId(refund.getId());
                    reversal.setPaymentAccount(original.getPaymentAccount());
                    reversal.setAmount(original.getAmount());
                    reversal.setDirection(TransactionDirectionEnum.IN);
                    reversal.setDescription("Reversal – " + original.getDescription());
                    reversal.setReversalOf(original);
                    transactionRepository.save(reversal);
                    journalService.reverseOnDate(JournalService.REF_SALE_RETURN_REFUND, refund.getId(), LocalDate.now());
                });
    }
}
