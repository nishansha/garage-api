package com.triasoft.garage.service.impl;

import com.triasoft.garage.dto.AccountBalanceLineDTO;
import com.triasoft.garage.dto.LedgerLineDTO;
import com.triasoft.garage.dto.TrialBalanceLineDTO;
import com.triasoft.garage.model.journal.LedgerRs;
import com.triasoft.garage.model.report.BalanceSheetRs;
import com.triasoft.garage.model.report.PLFromJournalRs;
import com.triasoft.garage.model.report.TrialBalanceRs;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * CSV renderers for the ledger-derived reports (Trial Balance, Balance Sheet,
 * P&L from journal). Each is a single multi-section CSV, mirroring
 * {@link PLReportCsvWriter}.
 */
@Component
public class JournalReportCsvWriter {

    public String trialBalanceCsv(TrialBalanceRs r) {
        StringBuilder sb = new StringBuilder();
        section(sb, "TRIAL BALANCE");
        row(sb, "As Of Date", str(r.getAsOfDate()));
        blank(sb);

        row(sb, "Code", "Account", "Type", "Debit", "Credit", "Net Balance", "Side");
        if (r.getLines() != null) {
            for (TrialBalanceLineDTO l : r.getLines()) {
                row(sb, l.getCode(), l.getName(), l.getType(), n(l.getTotalDebit()), n(l.getTotalCredit()),
                        n(l.getNetBalance()), l.getBalanceSide());
            }
        }
        row(sb, "Total", "", "", n(r.getTotalDebit()), n(r.getTotalCredit()), "", "");
        blank(sb);
        row(sb, "Balanced", yn(r.isBalanced()));
        return sb.toString();
    }

    public String balanceSheetCsv(BalanceSheetRs r) {
        StringBuilder sb = new StringBuilder();
        section(sb, "BALANCE SHEET");
        row(sb, "As Of Date", str(r.getAsOfDate()));
        blank(sb);

        section(sb, "ASSETS");
        accountBlock(sb, r.getAssets() != null ? r.getAssets().getAccounts() : null);
        row(sb, "Total Assets", "", r.getAssets() != null ? n(r.getAssets().getTotal()) : "0");
        blank(sb);

        section(sb, "LIABILITIES");
        accountBlock(sb, r.getLiabilities() != null ? r.getLiabilities().getAccounts() : null);
        row(sb, "Total Liabilities", "", r.getLiabilities() != null ? n(r.getLiabilities().getTotal()) : "0");
        blank(sb);

        section(sb, "EQUITY");
        accountBlock(sb, r.getEquity() != null ? r.getEquity().getAccounts() : null);
        if (r.getEquity() != null) {
            row(sb, "Current Year Earnings", "", n(r.getEquity().getCurrentYearEarnings()));
            row(sb, "Total Equity", "", n(r.getEquity().getTotal()));
        }
        blank(sb);

        row(sb, "Total Liabilities & Equity", "", n(r.getTotalLiabilitiesAndEquity()));
        row(sb, "Balanced", yn(r.isBalanced()));
        return sb.toString();
    }

    public String plFromJournalCsv(PLFromJournalRs r) {
        StringBuilder sb = new StringBuilder();
        section(sb, "PROFIT & LOSS (FROM LEDGER)");
        row(sb, "From Date", str(r.getFromDate()));
        row(sb, "To Date", str(r.getToDate()));
        blank(sb);

        section(sb, "REVENUE");
        accountBlock(sb, r.getRevenue() != null ? r.getRevenue().getAccounts() : null);
        row(sb, "Total Revenue", "", r.getRevenue() != null ? n(r.getRevenue().getTotal()) : "0");
        blank(sb);

        section(sb, "EXPENSES");
        accountBlock(sb, r.getExpenses() != null ? r.getExpenses().getAccounts() : null);
        row(sb, "Total Expenses", "", r.getExpenses() != null ? n(r.getExpenses().getTotal()) : "0");
        blank(sb);

        row(sb, "Net Profit", "", n(r.getNetProfit()));
        return sb.toString();
    }

    public String ledgerCsv(LedgerRs r) {
        StringBuilder sb = new StringBuilder();
        section(sb, "GENERAL LEDGER");
        AccountBalanceLineDTO acc = r.getAccount();
        if (acc != null) {
            row(sb, "Account", str(acc.getCode()) + " " + str(acc.getLabel()));
            row(sb, "Type", acc.getType());
        }
        row(sb, "Period", str(r.getFromDate()) + " to " + str(r.getToDate()));
        row(sb, "Opening Balance", n(r.getOpeningBalance()), r.getOpeningBalanceSide());
        row(sb, "Closing Balance", n(r.getClosingBalance()), r.getClosingBalanceSide());
        row(sb, "Total Debit", n(r.getTotalDebit()));
        row(sb, "Total Credit", n(r.getTotalCredit()));
        blank(sb);

        row(sb, "Date", "Journal ID", "Reference", "Description", "Debit", "Credit", "Running Balance", "Side");
        if (r.getLines() != null) {
            for (LedgerLineDTO l : r.getLines()) {
                String ref = l.getReferenceType() == null ? "" : l.getReferenceType() + " #" + str(l.getReferenceId());
                row(sb, str(l.getJournalDate()), l.getJournalId(), ref, l.getDescription(),
                        n(l.getDebit()), n(l.getCredit()), n(l.getRunningBalance()), l.getRunningBalanceSide());
            }
        }
        return sb.toString();
    }

    private void accountBlock(StringBuilder sb, List<AccountBalanceLineDTO> accounts) {
        row(sb, "Code", "Account", "Balance");
        if (accounts != null) {
            for (AccountBalanceLineDTO a : accounts) {
                row(sb, a.getCode(), a.getLabel(), n(a.getBalance()));
            }
        }
    }

    private void section(StringBuilder sb, String title) {
        row(sb, title);
    }

    private void blank(StringBuilder sb) {
        sb.append("\r\n");
    }

    private void row(StringBuilder sb, Object... cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(escape(cells[i]));
        }
        sb.append("\r\n");
    }

    private String n(BigDecimal v) {
        return v != null ? v.toPlainString() : "0";
    }

    private String str(Object v) {
        return v != null ? String.valueOf(v) : "";
    }

    private String yn(boolean b) {
        return b ? "Yes" : "No";
    }

    private String escape(Object cell) {
        if (cell == null) {
            return "";
        }
        String s = String.valueOf(cell);
        if (s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0) {
            return '"' + s.replace("\"", "\"\"") + '"';
        }
        return s;
    }
}
