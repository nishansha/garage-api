package com.triasoft.garage.service.impl;

import com.triasoft.garage.concurrency.VersionCheck;

import com.triasoft.garage.constants.AccountTypeEnum;
import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.constants.TransactionDirectionEnum;
import com.triasoft.garage.dto.PaymentAccountDTO;
import com.triasoft.garage.dto.TransactionDTO;
import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.entity.PaymentAccount;
import com.triasoft.garage.entity.Transaction;
import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.model.payment.PaymentAccountRq;
import com.triasoft.garage.model.payment.PaymentAccountRs;
import com.triasoft.garage.model.payment.ReconcileRq;
import com.triasoft.garage.model.payment.ReconcileRs;
import com.triasoft.garage.model.payment.TransactionRs;
import com.triasoft.garage.entity.ChartOfAccount;
import com.triasoft.garage.repository.ChartOfAccountRepository;
import com.triasoft.garage.repository.PaymentAccountRepository;
import com.triasoft.garage.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentAccountService {

    private final PaymentAccountRepository paymentAccountRepository;
    private final TransactionRepository transactionRepository;
    private final ChartOfAccountRepository chartOfAccountRepository;
    private final JournalService journalService;

    public PaymentAccountRs getAll() {
        List<PaymentAccountDTO> accounts = paymentAccountRepository.findAllByIsActiveTrue()
                .stream()
                .map(a -> toDTO(a, null))
                .toList();
        return PaymentAccountRs.builder().accounts(accounts).build();
    }

    public PaymentAccountRs getAllWithBalance() {
        List<PaymentAccountDTO> accounts = paymentAccountRepository.findAllByIsActiveTrue()
                .stream()
                .map(a -> toDTO(a, computeBalance(a)))
                .toList();
        return PaymentAccountRs.builder().accounts(accounts).build();
    }

    public PaymentAccountDTO get(Long id) {
        PaymentAccount account = findById(id);
        return toDTO(account, computeBalance(account));
    }

    @Transactional
    public PaymentAccountRs create(PaymentAccountRq rq, UserDTO user) {
        if (paymentAccountRepository.existsByNameIgnoreCase(rq.getName())) {
            throw new BusinessException(ErrorCode.Business.PAYMENT_ACCOUNT_NAME_EXISTS);
        }
        if (StringUtils.hasText(rq.getAccountNo()) && paymentAccountRepository.existsByAccountNoIgnoreCase(rq.getAccountNo())) {
            throw new BusinessException(ErrorCode.Business.PAYMENT_ACCOUNT_NO_EXISTS);
        }
        PaymentAccount account = new PaymentAccount();
        account.setName(rq.getName());
        account.setBankName(rq.getBankName());
        account.setAccountNo(rq.getAccountNo());
        account.setIfscCode(StringUtils.hasLength(rq.getIfscCode()) ? rq.getIfscCode().toUpperCase(): null);
        account.setAccountType(rq.getAccountType());
        account.setOpeningBalance(rq.getOpeningBalance() != null ? rq.getOpeningBalance() : BigDecimal.ZERO);
        account.setActive(rq.getIsActive() != null ? rq.getIsActive() : true);
        account.setOpeningDate(rq.getOpeningDate());
        PaymentAccount saved = paymentAccountRepository.save(account);
        saved.setChartOfAccount(autoCreateCoA(saved));
        saved = paymentAccountRepository.save(saved);
        if (saved.getOpeningBalance() != null && saved.getOpeningBalance().signum() > 0) {
            journalService.post(JournalService.REF_OPENING_BALANCE, saved.getId());
        }
        return PaymentAccountRs.builder().id(saved.getId()).build();
    }

    @Transactional
    @VersionCheck(entity = PaymentAccount.class)
    public PaymentAccountRs update(Long id, PaymentAccountRq rq, UserDTO user) {
        PaymentAccount account = findById(id);
        if (!account.getName().equalsIgnoreCase(rq.getName()) && paymentAccountRepository.existsByNameIgnoreCase(rq.getName())) {
            throw new BusinessException(ErrorCode.Business.PAYMENT_ACCOUNT_NAME_EXISTS);
        }
        if (StringUtils.hasText(rq.getAccountNo()) && paymentAccountRepository.existsByAccountNoIgnoreCaseAndIdNot(rq.getAccountNo(), id)) {
            throw new BusinessException(ErrorCode.Business.PAYMENT_ACCOUNT_NO_EXISTS);
        }
        if (rq.getOpeningBalance() != null && rq.getOpeningBalance().compareTo(account.getOpeningBalance()) != 0) {
            if (transactionRepository.existsByPaymentAccountId(account.getId())) {
                throw new BusinessException(ErrorCode.Business.OPENING_BALANCE_LOCKED);
            }
            account.setOpeningBalance(rq.getOpeningBalance());
            journalService.reverse(JournalService.REF_OPENING_BALANCE, account.getId());
            if (account.getOpeningBalance().signum() > 0) {
                journalService.post(JournalService.REF_OPENING_BALANCE, account.getId());
            }
        }
        if (!account.getName().equalsIgnoreCase(rq.getName()) && account.getChartOfAccount() != null) {
            account.getChartOfAccount().setLabel(rq.getName());
            chartOfAccountRepository.save(account.getChartOfAccount());
        }
        account.setName(rq.getName());
        account.setBankName(rq.getBankName());
        account.setAccountNo(rq.getAccountNo());
        account.setIfscCode(StringUtils.hasLength(rq.getIfscCode()) ? rq.getIfscCode().toUpperCase(): null);
        if (rq.getIsActive() != null) {
            account.setActive(rq.getIsActive());
        }
        paymentAccountRepository.save(account);
        return PaymentAccountRs.builder().build();
    }

    public TransactionRs getTransactions(Long accountId, Pageable pageable) {
        findById(accountId);
        Page<Transaction> page = transactionRepository.findByPaymentAccountIdOrderByTransactionDateDescCreatedAtDesc(accountId, pageable);
        List<TransactionDTO> transactions = page.getContent().stream()
                .map(t -> toTransactionDTO(t, transactionRepository.existsByReversalOfId(t.getId())))
                .toList();
        TransactionRs rs = TransactionRs.builder().transactions(transactions).build();
        rs.setTotalPages(page.getTotalPages());
        rs.setTotalElements(page.getTotalElements());
        return rs;
    }

    @Transactional
    public TransactionRs reverse(Long transactionId, UserDTO user) {
        Transaction original = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.TRANSACTION_NOT_FOUND));
        if (transactionRepository.existsByReversalOfId(transactionId)) {
            throw new BusinessException(ErrorCode.Business.TRANSACTION_ALREADY_REVERSED);
        }
        Transaction reversal = new Transaction();
        reversal.setTransactionDate(original.getTransactionDate());
        reversal.setType(original.getType());
        reversal.setReferenceType(original.getReferenceType());
        reversal.setReferenceId(original.getReferenceId());
        reversal.setPaymentAccount(original.getPaymentAccount());
        reversal.setAmount(original.getAmount());
        reversal.setDirection(original.getDirection() == TransactionDirectionEnum.IN ? TransactionDirectionEnum.OUT : TransactionDirectionEnum.IN);
        reversal.setDescription("Reversal of: " + original.getDescription());
        reversal.setNotes("System reversal");
        reversal.setReversalOf(original);
        transactionRepository.save(reversal);

        // TODO [JOURNAL ENTRY] - Transaction Reversal
        // Trigger  : when any transaction is reversed via this method.
        // Entry    : exact mirror of the original transaction's journal entry with Dr/Cr swapped.
        //   Example (if original was a purchase payment):
        //   Original:  Dr Accounts Payable (Liability)  /  Cr Bank Account (Asset)
        //   Reversal:  Dr Bank Account (Asset)          /  Cr Accounts Payable (Liability)
        // Note: The app_transaction reversal row is already saved above.
        //       The journal reversal must reference the same source (referenceType + referenceId).
        // Future call: JournalEntryService.reverseByReference(original.getReferenceType(), original.getReferenceId())

        return TransactionRs.builder().build();
    }

    @Transactional
    public ReconcileRs reconcile(Long accountId, ReconcileRq rq) {
        findById(accountId);
        if (rq.getTransactionIds() == null || rq.getTransactionIds().isEmpty()) {
            throw new BusinessException("REC_400", "At least one transaction ID is required");
        }
        LocalDate today = LocalDate.now();
        int reconciled = 0;
        int alreadyReconciled = 0;
        int skipped = 0;
        for (Long txnId : rq.getTransactionIds()) {
            Transaction txn = transactionRepository.findById(txnId).orElse(null);
            if (txn == null || !accountId.equals(txn.getPaymentAccount() != null ? txn.getPaymentAccount().getId() : null)) {
                skipped++;
                continue;
            }
            if (txn.isReconciled()) {
                alreadyReconciled++;
                continue;
            }
            txn.setReconciled(true);
            txn.setReconciledAt(today);
            transactionRepository.save(txn);
            reconciled++;
        }
        return ReconcileRs.builder()
                .totalRequested(rq.getTransactionIds().size())
                .reconciled(reconciled)
                .alreadyReconciled(alreadyReconciled)
                .skipped(skipped)
                .build();
    }

    public TransactionRs getUnreconciledTransactions(Long accountId, Pageable pageable) {
        findById(accountId);
        Page<Transaction> page = transactionRepository
                .findByPaymentAccountIdAndReconciledFalseOrderByTransactionDateDescCreatedAtDesc(accountId, pageable);
        List<TransactionDTO> transactions = page.getContent().stream()
                .map(t -> toTransactionDTO(t, transactionRepository.existsByReversalOfId(t.getId())))
                .toList();
        TransactionRs rs = TransactionRs.builder().transactions(transactions).build();
        rs.setTotalPages(page.getTotalPages());
        rs.setTotalElements(page.getTotalElements());
        return rs;
    }

    private ChartOfAccount autoCreateCoA(PaymentAccount account) {
        boolean isBank = AccountTypeEnum.BANK.equals(account.getAccountType());
        long nextCode = chartOfAccountRepository.findMaxNumericCodeByType("ASSET") + 1;
        ChartOfAccount coa = new ChartOfAccount();
        coa.setType("ASSET");
        coa.setName(isBank ? "A-BNK-" + account.getId() : "A-CSH-" + account.getId());
        coa.setCode(String.valueOf(nextCode));
        coa.setLabel(account.getName());
        coa.setDescription((isBank ? "Bank" : "Cash") + " account — " + account.getName());
        coa.setControlEnabled(false);
        coa.setDirectPostable(false);
        return chartOfAccountRepository.save(coa);
    }

    public PaymentAccount findById(Long id) {
        return paymentAccountRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.PAYMENT_ACCOUNT_NOT_FOUND));
    }

    private BigDecimal computeBalance(PaymentAccount account) {
        BigDecimal totalIn = transactionRepository.sumAmountByAccountAndDirection(account.getId(), TransactionDirectionEnum.IN);
        BigDecimal totalOut = transactionRepository.sumAmountByAccountAndDirection(account.getId(), TransactionDirectionEnum.OUT);
        return account.getOpeningBalance().add(totalIn).subtract(totalOut);
    }

    private PaymentAccountDTO toDTO(PaymentAccount account, BigDecimal balance) {
        return PaymentAccountDTO.builder()
                .id(account.getId())
                .version(account.getVersion())
                .name(account.getName())
                .bankName(account.getBankName())
                .accountNo(account.getAccountNo())
                .ifscCode(account.getIfscCode())
                .accountType(account.getAccountType())
                .openingBalance(account.getOpeningBalance())
                .currentBalance(balance)
                .isActive(account.isActive())
                .coaId(account.getChartOfAccount() != null ? account.getChartOfAccount().getId() : null)
                .coaLabel(account.getChartOfAccount() != null ? account.getChartOfAccount().getLabel() : null)
                .build();
    }

    private TransactionDTO toTransactionDTO(Transaction t, boolean isReversed) {
        return TransactionDTO.builder()
                .id(t.getId())
                .transactionDate(t.getTransactionDate())
                .type(t.getType())
                .referenceType(t.getReferenceType())
                .referenceId(t.getReferenceId())
                .paymentAccountId(t.getPaymentAccount() != null ? t.getPaymentAccount().getId() : null)
                .paymentAccountName(t.getPaymentAccount() != null ? t.getPaymentAccount().getName() : null)
                .amount(t.getAmount())
                .direction(t.getDirection())
                .description(t.getDescription())
                .notes(t.getNotes())
                .reversalOfId(t.getReversalOf() != null ? t.getReversalOf().getId() : null)
                .isReversed(isReversed)
                .reconciled(t.isReconciled())
                .reconciledAt(t.getReconciledAt())
                .createdAt(t.getCreatedAt())
                .build();
    }

}
