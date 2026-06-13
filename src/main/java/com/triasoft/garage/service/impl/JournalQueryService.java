package com.triasoft.garage.service.impl;

import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.constants.JournalStatusEnum;
import com.triasoft.garage.dto.AccountBalanceLineDTO;
import com.triasoft.garage.dto.JournalDTO;
import com.triasoft.garage.dto.JournalLineDTO;
import com.triasoft.garage.dto.LedgerLineDTO;
import com.triasoft.garage.dto.TrialBalanceLineDTO;
import com.triasoft.garage.entity.ChartOfAccount;
import com.triasoft.garage.entity.Journal;
import com.triasoft.garage.entity.JournalDetail;
import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.model.journal.JournalDetailRs;
import com.triasoft.garage.model.journal.JournalListRs;
import com.triasoft.garage.model.journal.LedgerRs;
import com.triasoft.garage.model.report.BalanceSheetRs;
import com.triasoft.garage.model.report.PLFromJournalRs;
import com.triasoft.garage.model.report.TrialBalanceRs;
import com.triasoft.garage.projection.AccountBalanceRow;
import com.triasoft.garage.projection.LedgerRow;
import com.triasoft.garage.repository.ChartOfAccountRepository;
import com.triasoft.garage.repository.JournalDetailRepository;
import com.triasoft.garage.repository.JournalRepository;
import com.triasoft.garage.specifiction.JournalSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JournalQueryService {

    private final JournalRepository journalRepository;
    private final JournalDetailRepository journalDetailRepository;
    private final ChartOfAccountRepository chartOfAccountRepository;

    // ─────────────────────────────────────────────────────────────────────────
    //  Journal list / detail
    // ─────────────────────────────────────────────────────────────────────────

    public JournalListRs list(String referenceType, JournalStatusEnum status,
                              LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        Page<Journal> page = journalRepository.findAll(
                JournalSpecification.build(referenceType, status, fromDate, toDate), pageable);
        List<JournalDTO> journals = page.getContent().stream().map(this::toJournalDTO).toList();
        JournalListRs rs = JournalListRs.builder().journals(journals).build();
        rs.setTotalPages(page.getTotalPages());
        rs.setTotalElements(page.getTotalElements());
        return rs;
    }

    public JournalDetailRs getById(Long id) {
        Journal journal = journalRepository.findById(id)
                .orElseThrow(() -> new BusinessException("JNL_404", "Journal not found"));
        return toJournalDetailRs(journal);
    }

    public JournalDetailRs getByReference(String referenceType, Long referenceId) {
        Journal journal = journalRepository.findActiveByReferenceTypeAndReferenceId(referenceType, referenceId)
                .or(() -> journalRepository.findLatestByReferenceTypeAndReferenceId(referenceType, referenceId))
                .orElseThrow(() -> new BusinessException("JNL_404",
                        "No journal found for " + referenceType + " #" + referenceId));
        return toJournalDetailRs(journal);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  General Ledger
    // ─────────────────────────────────────────────────────────────────────────

    public LedgerRs getLedger(Long accountId, LocalDate fromDate, LocalDate toDate) {
        ChartOfAccount account = chartOfAccountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.JOURNAL_COA_MISSING));

        LocalDate effFrom = fromDate != null ? fromDate : LocalDate.of(1970, 1, 1);
        LocalDate effTo = toDate != null ? toDate : LocalDate.now();

        var openingRow = journalDetailRepository.getOpeningBalance(accountId, effFrom);
        BigDecimal openingDr = safe(openingRow.getDebit());
        BigDecimal openingCr = safe(openingRow.getCredit());
        BigDecimal openingBalance = balanceFor(account.getType(), openingDr, openingCr);
        String openingSide = balanceSide(account.getType(), openingDr, openingCr);

        List<LedgerRow> rows = journalDetailRepository.getLedgerEntries(accountId, effFrom, effTo);
        BigDecimal totalDr = BigDecimal.ZERO;
        BigDecimal totalCr = BigDecimal.ZERO;
        BigDecimal running = openingBalance;
        // Convert running to a signed value where DR is positive
        boolean drNormal = isDrNormal(account.getType());
        BigDecimal signedRunning = drNormal ? running : running.negate();

        List<LedgerLineDTO> lines = new ArrayList<>();
        for (LedgerRow row : rows) {
            BigDecimal dr = safe(row.getDebit());
            BigDecimal cr = safe(row.getCredit());
            totalDr = totalDr.add(dr);
            totalCr = totalCr.add(cr);
            signedRunning = signedRunning.add(dr).subtract(cr);
            BigDecimal displayRunning = drNormal ? signedRunning : signedRunning.negate();
            String runningSide = displayRunning.signum() >= 0
                    ? (drNormal ? "DR" : "CR")
                    : (drNormal ? "CR" : "DR");
            lines.add(LedgerLineDTO.builder()
                    .journalId(row.getJournalId())
                    .journalDate(row.getJournalDate())
                    .referenceType(row.getReferenceType())
                    .referenceId(row.getReferenceId())
                    .description(row.getLineDescription() != null ? row.getLineDescription() : row.getJournalDescription())
                    .debit(dr)
                    .credit(cr)
                    .runningBalance(displayRunning.abs())
                    .runningBalanceSide(runningSide)
                    .build());
        }

        BigDecimal closingDr = openingDr.add(totalDr);
        BigDecimal closingCr = openingCr.add(totalCr);
        BigDecimal closingBalance = balanceFor(account.getType(), closingDr, closingCr);
        String closingSide = balanceSide(account.getType(), closingDr, closingCr);

        return LedgerRs.builder()
                .account(AccountBalanceLineDTO.builder()
                        .accountId(account.getId())
                        .code(account.getCode())
                        .label(account.getLabel())
                        .type(account.getType())
                        .balance(closingBalance)
                        .build())
                .fromDate(effFrom)
                .toDate(effTo)
                .openingBalance(openingBalance.abs())
                .openingBalanceSide(openingSide)
                .closingBalance(closingBalance.abs())
                .closingBalanceSide(closingSide)
                .totalDebit(totalDr)
                .totalCredit(totalCr)
                .lines(lines)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Trial Balance
    // ─────────────────────────────────────────────────────────────────────────

    public TrialBalanceRs getTrialBalance(LocalDate asOfDate, boolean includeZeroBalance) {
        LocalDate effDate = asOfDate != null ? asOfDate : LocalDate.now();
        List<AccountBalanceRow> rows = journalDetailRepository.getTrialBalance(effDate);

        List<TrialBalanceLineDTO> lines = new ArrayList<>();
        BigDecimal totalDr = BigDecimal.ZERO;
        BigDecimal totalCr = BigDecimal.ZERO;
        for (AccountBalanceRow row : rows) {
            BigDecimal dr = safe(row.getTotalDebit());
            BigDecimal cr = safe(row.getTotalCredit());
            BigDecimal net = balanceFor(row.getType(), dr, cr);
            String side = balanceSide(row.getType(), dr, cr);
            if (!includeZeroBalance && net.signum() == 0 && dr.signum() == 0 && cr.signum() == 0) {
                continue;
            }
            totalDr = totalDr.add(dr);
            totalCr = totalCr.add(cr);
            lines.add(TrialBalanceLineDTO.builder()
                    .accountId(row.getAccountId())
                    .code(row.getCode())
                    .name(row.getName())
                    .label(row.getLabel())
                    .type(row.getType())
                    .totalDebit(dr)
                    .totalCredit(cr)
                    .netBalance(net.abs())
                    .balanceSide(side)
                    .build());
        }

        return TrialBalanceRs.builder()
                .asOfDate(effDate)
                .lines(lines)
                .totalDebit(totalDr)
                .totalCredit(totalCr)
                .isBalanced(totalDr.compareTo(totalCr) == 0)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Balance Sheet
    // ─────────────────────────────────────────────────────────────────────────

    public BalanceSheetRs getBalanceSheet(LocalDate asOfDate) {
        LocalDate effDate = asOfDate != null ? asOfDate : LocalDate.now();
        List<AccountBalanceRow> rows = journalDetailRepository.getTrialBalance(effDate);

        List<AccountBalanceLineDTO> assetLines = new ArrayList<>();
        List<AccountBalanceLineDTO> liabilityLines = new ArrayList<>();
        List<AccountBalanceLineDTO> equityLines = new ArrayList<>();
        BigDecimal assetsTotal = BigDecimal.ZERO;
        BigDecimal liabilitiesTotal = BigDecimal.ZERO;
        BigDecimal equityTotal = BigDecimal.ZERO;
        BigDecimal revenueTotal = BigDecimal.ZERO;
        BigDecimal expenseTotal = BigDecimal.ZERO;

        for (AccountBalanceRow row : rows) {
            BigDecimal dr = safe(row.getTotalDebit());
            BigDecimal cr = safe(row.getTotalCredit());
            BigDecimal net = balanceFor(row.getType(), dr, cr);
            if (net.signum() == 0) continue;
            AccountBalanceLineDTO line = AccountBalanceLineDTO.builder()
                    .accountId(row.getAccountId())
                    .code(row.getCode())
                    .label(row.getLabel())
                    .type(row.getType())
                    .balance(net.abs())
                    .build();
            switch (row.getType()) {
                case "ASSET" -> {
                    assetLines.add(line);
                    assetsTotal = assetsTotal.add(net);
                }
                case "LIABILITY" -> {
                    liabilityLines.add(line);
                    liabilitiesTotal = liabilitiesTotal.add(net);
                }
                case "EQUITY" -> {
                    equityLines.add(line);
                    equityTotal = equityTotal.add(net);
                }
                case "REVENUE" -> revenueTotal = revenueTotal.add(net);
                case "EXPENSE" -> expenseTotal = expenseTotal.add(net);
                default -> {}
            }
        }

        BigDecimal currentYearEarnings = revenueTotal.subtract(expenseTotal);
        BigDecimal totalEquity = equityTotal.add(currentYearEarnings);
        BigDecimal totalLiabAndEquity = liabilitiesTotal.add(totalEquity);

        return BalanceSheetRs.builder()
                .asOfDate(effDate)
                .assets(BalanceSheetRs.Section.builder()
                        .accounts(assetLines)
                        .total(assetsTotal)
                        .build())
                .liabilities(BalanceSheetRs.Section.builder()
                        .accounts(liabilityLines)
                        .total(liabilitiesTotal)
                        .build())
                .equity(BalanceSheetRs.EquitySection.builder()
                        .accounts(equityLines)
                        .currentYearEarnings(currentYearEarnings)
                        .total(totalEquity)
                        .build())
                .totalLiabilitiesAndEquity(totalLiabAndEquity)
                .isBalanced(assetsTotal.compareTo(totalLiabAndEquity) == 0)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  P&L from Journal
    // ─────────────────────────────────────────────────────────────────────────

    public PLFromJournalRs getPLFromJournal(LocalDate fromDate, LocalDate toDate) {
        LocalDate effFrom = fromDate != null ? fromDate : LocalDate.now().withDayOfMonth(1);
        LocalDate effTo = toDate != null ? toDate : LocalDate.now();
        List<AccountBalanceRow> rows = journalDetailRepository.getPLAccountBalances(effFrom, effTo);

        List<AccountBalanceLineDTO> revenueLines = new ArrayList<>();
        List<AccountBalanceLineDTO> expenseLines = new ArrayList<>();
        BigDecimal revenueTotal = BigDecimal.ZERO;
        BigDecimal expenseTotal = BigDecimal.ZERO;

        for (AccountBalanceRow row : rows) {
            BigDecimal dr = safe(row.getTotalDebit());
            BigDecimal cr = safe(row.getTotalCredit());
            BigDecimal net = balanceFor(row.getType(), dr, cr);
            if (net.signum() == 0) continue;
            AccountBalanceLineDTO line = AccountBalanceLineDTO.builder()
                    .accountId(row.getAccountId())
                    .code(row.getCode())
                    .label(row.getLabel())
                    .type(row.getType())
                    .balance(net.abs())
                    .build();
            if ("REVENUE".equals(row.getType())) {
                revenueLines.add(line);
                revenueTotal = revenueTotal.add(net);
            } else if ("EXPENSE".equals(row.getType())) {
                expenseLines.add(line);
                expenseTotal = expenseTotal.add(net);
            }
        }

        BigDecimal netProfit = revenueTotal.subtract(expenseTotal);

        return PLFromJournalRs.builder()
                .fromDate(effFrom)
                .toDate(effTo)
                .revenue(PLFromJournalRs.Section.builder()
                        .accounts(revenueLines)
                        .total(revenueTotal)
                        .build())
                .expenses(PLFromJournalRs.Section.builder()
                        .accounts(expenseLines)
                        .total(expenseTotal)
                        .build())
                .netProfit(netProfit)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private JournalDTO toJournalDTO(Journal j) {
        var totals = journalDetailRepository.getJournalTotals(j.getId());
        BigDecimal amount = safe(totals.getDebit());
        return JournalDTO.builder()
                .id(j.getId())
                .journalDate(j.getJournalDate())
                .referenceType(j.getReferenceType())
                .referenceId(j.getReferenceId())
                .description(j.getDescription())
                .status(j.getStatus())
                .reversalOfId(j.getReversalOf() != null ? j.getReversalOf().getId() : null)
                .totalAmount(amount)
                .createdAt(j.getCreatedAt())
                .build();
    }

    private JournalDetailRs toJournalDetailRs(Journal j) {
        List<JournalDetail> lines = journalDetailRepository.findByJournalId(j.getId());
        BigDecimal totalDr = BigDecimal.ZERO;
        BigDecimal totalCr = BigDecimal.ZERO;
        List<JournalLineDTO> lineDTOs = new ArrayList<>();
        for (JournalDetail line : lines) {
            BigDecimal dr = safe(line.getDebitAmount());
            BigDecimal cr = safe(line.getCreditAmount());
            totalDr = totalDr.add(dr);
            totalCr = totalCr.add(cr);
            lineDTOs.add(JournalLineDTO.builder()
                    .id(line.getId())
                    .accountId(line.getAccount().getId())
                    .accountCode(line.getAccount().getCode())
                    .accountName(line.getAccount().getName())
                    .accountLabel(line.getAccount().getLabel())
                    .accountType(line.getAccount().getType())
                    .debitAmount(dr)
                    .creditAmount(cr)
                    .description(line.getDescription())
                    .build());
        }
        return JournalDetailRs.builder()
                .id(j.getId())
                .journalDate(j.getJournalDate())
                .referenceType(j.getReferenceType())
                .referenceId(j.getReferenceId())
                .description(j.getDescription())
                .status(j.getStatus())
                .reversalOfId(j.getReversalOf() != null ? j.getReversalOf().getId() : null)
                .totalDebit(totalDr)
                .totalCredit(totalCr)
                .createdAt(j.getCreatedAt())
                .lines(lineDTOs)
                .build();
    }

    private boolean isDrNormal(String accountType) {
        return "ASSET".equals(accountType) || "EXPENSE".equals(accountType);
    }

    /** Returns signed balance — positive for natural-side balance, negative otherwise. */
    private BigDecimal balanceFor(String accountType, BigDecimal dr, BigDecimal cr) {
        return isDrNormal(accountType) ? dr.subtract(cr) : cr.subtract(dr);
    }

    private String balanceSide(String accountType, BigDecimal dr, BigDecimal cr) {
        BigDecimal net = dr.subtract(cr);
        if (net.signum() == 0) return isDrNormal(accountType) ? "DR" : "CR";
        return net.signum() > 0 ? "DR" : "CR";
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

}
