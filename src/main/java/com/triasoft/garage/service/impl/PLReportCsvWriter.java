package com.triasoft.garage.service.impl;

import com.triasoft.garage.model.report.AccountBalanceInfo;
import com.triasoft.garage.model.report.DirectEntryLineInfo;
import com.triasoft.garage.model.report.DirectEntryTotals;
import com.triasoft.garage.model.report.ExpenseLineInfo;
import com.triasoft.garage.model.report.ExpenseTotals;
import com.triasoft.garage.model.report.PLReportRs;
import com.triasoft.garage.model.report.PurchaseLineInfo;
import com.triasoft.garage.model.report.PurchaseTotals;
import com.triasoft.garage.model.report.SaleLineInfo;
import com.triasoft.garage.model.report.SalesTotals;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Renders a {@link PLReportRs} as a single multi-section CSV — every summary
 * figure and every detail list, each section with its own header row, separated
 * by blank lines (the way accounting tools export composite reports).
 */
@Component
public class PLReportCsvWriter {

    public String toCsv(PLReportRs r) {
        StringBuilder sb = new StringBuilder();

        section(sb, "MONTHLY BUSINESS SUMMARY");
        row(sb, "Month", r.getMonth());
        row(sb, "Period", r.getPeriod());
        blank(sb);

        section(sb, "P&L SUMMARY");
        row(sb, "Metric", "Amount");
        row(sb, "Total Revenue", n(r.getTotalRevenue()));
        row(sb, "Gross Profit", n(r.getGrossProfit()));
        row(sb, "Gross Margin %", String.valueOf(r.getGrossMarginPct()));
        row(sb, "Return Deduction Income", n(r.getReturnDeductionIncome()));
        row(sb, "Exchange Gain", n(r.getExchangeGain()));
        row(sb, "Exchange Return Loss", n(r.getExchangeReturnLoss()));
        row(sb, "Purchase Return Loss", n(r.getPurchaseReturnLoss()));
        row(sb, "Total Operating Expenses", n(r.getTotalOperatingExpenses()));
        row(sb, "Net Profit", n(r.getNetProfit()));
        row(sb, "Net Margin %", String.valueOf(r.getNetMarginPct()));
        blank(sb);

        SalesTotals st = r.getSalesTotals();
        section(sb, "SALES TOTALS");
        row(sb, "Count", "Return Count", "Sale Rate", "Cost", "Purchase Expenses", "Profit");
        if (st != null) {
            row(sb, st.getCount(), st.getReturnCount(), n(st.getSaleRate()), n(st.getCost()),
                    n(st.getPurchaseExpenses()), n(st.getProfit()));
        }
        blank(sb);

        PurchaseTotals pt = r.getPurchaseTotals();
        section(sb, "PURCHASE TOTALS");
        row(sb, "Count", "Return Count", "Purchase Rate", "Purchase Expenses", "Landed Cost", "Return Amount");
        if (pt != null) {
            row(sb, pt.getCount(), pt.getReturnCount(), n(pt.getPurchaseRate()), n(pt.getPurchaseExpenses()),
                    n(pt.getLandedCost()), n(pt.getReturnAmount()));
        }
        blank(sb);

        ExpenseTotals et = r.getExpenseTotals();
        section(sb, "EXPENSE TOTALS");
        row(sb, "Count", "Amount");
        if (et != null) {
            row(sb, et.getCount(), n(et.getAmount()));
        }
        blank(sb);

        DirectEntryTotals dt = r.getDirectEntryTotals();
        section(sb, "DIRECT ENTRY TOTALS");
        row(sb, "In Count", "In Amount", "Out Count", "Out Amount",
                "Income Count", "Income Amount", "Expense Count", "Expense Amount", "Other Count", "Other Amount");
        if (dt != null) {
            row(sb, dt.getInCount(), n(dt.getInAmount()), dt.getOutCount(), n(dt.getOutAmount()),
                    dt.getIncomeCount(), n(dt.getIncomeAmount()), dt.getExpenseCount(), n(dt.getExpenseAmount()),
                    dt.getOtherCount(), n(dt.getOtherAmount()));
        }
        blank(sb);

        section(sb, "RECEIVABLES & PAYABLES");
        row(sb, "Metric", "Amount");
        row(sb, "Total Receivables (month-end)", n(r.getTotalReceivables()));
        row(sb, "Total Receivables (till date)", n(r.getTotalReceivablesTillDate()));
        row(sb, "Total Payables (month-end)", n(r.getTotalPayables()));
        row(sb, "Total Payables (till date)", n(r.getTotalPayablesTillDate()));
        blank(sb);

        section(sb, "CASH POSITION (as of month-end)");
        row(sb, "Account", "Type", "Balance");
        if (r.getCashPosition() != null) {
            for (AccountBalanceInfo a : r.getCashPosition()) {
                row(sb, a.getName(), a.getAccountType(), n(a.getBalance()));
            }
        }
        row(sb, "Total", "", n(r.getTotalCashPosition()));
        blank(sb);

        section(sb, "SALES");
        row(sb, "Sale ID", "Invoice No", "Date", "Vehicle No", "Customer",
                "Purchase Rate", "Purchase Expenses", "Sale Rate", "Profit", "Returned", "Pending Amount");
        if (r.getSales() != null) {
            for (SaleLineInfo s : r.getSales()) {
                row(sb, s.getSaleId(), s.getInvoiceNo(), s.getSaleDate(), s.getVehicleNo(), s.getCustomerName(),
                        n(s.getPurchaseRate()), n(s.getPurchaseExpenses()), n(s.getSaleRate()), n(s.getProfit()),
                        yn(s.isReturned()), n(s.getPendingAmount()));
            }
        }
        blank(sb);

        section(sb, "PURCHASES");
        row(sb, "Purchase ID", "Reference No", "Date", "Vehicle No", "Vendor",
                "Purchase Rate", "Purchase Expenses", "Landed Cost", "Returned", "Return Amount", "Pending Amount");
        if (r.getPurchases() != null) {
            for (PurchaseLineInfo p : r.getPurchases()) {
                row(sb, p.getPurchaseId(), p.getReferenceNo(), p.getPurchaseDate(), p.getVehicleNo(), p.getVendorName(),
                        n(p.getPurchaseRate()), n(p.getPurchaseExpenses()), n(p.getLandedCost()),
                        yn(p.isReturned()), n(p.getReturnAmount()), n(p.getPendingAmount()));
            }
        }
        blank(sb);

        section(sb, "EXPENSES");
        row(sb, "Date", "Expense Name", "Amount", "Account");
        if (r.getExpenses() != null) {
            for (ExpenseLineInfo e : r.getExpenses()) {
                row(sb, e.getDate(), e.getExpenseName(), n(e.getAmount()), e.getAccountName());
            }
        }
        blank(sb);

        section(sb, "DIRECT ENTRIES");
        row(sb, "Date", "Name", "Amount", "Category", "Account", "Direction", "Classification");
        if (r.getDirectEntries() != null) {
            for (DirectEntryLineInfo d : r.getDirectEntries()) {
                row(sb, d.getDate(), d.getName(), n(d.getAmount()), d.getCategory(),
                        d.getAccountName(), d.getDirection(), d.getClassification());
            }
        }

        return sb.toString();
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
