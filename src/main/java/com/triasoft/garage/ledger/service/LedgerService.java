package com.triasoft.garage.ledger.service;

import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.ledger.constants.JournalStatusEnum;
import com.triasoft.garage.ledger.entity.ChartOfAccount;
import com.triasoft.garage.ledger.entity.Journal;
import com.triasoft.garage.ledger.entity.JournalDetail;
import com.triasoft.garage.ledger.repository.ChartOfAccountRepository;
import com.triasoft.garage.ledger.repository.JournalDetailRepository;
import com.triasoft.garage.ledger.repository.JournalRepository;
import com.triasoft.garage.model.journal.JournalRq;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Generic double-entry ledger mechanics - post/reverse balanced journals, look up accounts.
 * Deliberately knows nothing about Sale/Purchase/Expense/etc: callers (e.g. garage-api's
 * JournalService) decide WHAT lines to post; this class only knows HOW to post them and
 * keep them balanced. Kept free of any domain-specific entity so it can move into a
 * standalone reusable module later with no further rework.
 */
@Service
@RequiredArgsConstructor
public class LedgerService {

    private final JournalRepository journalRepository;
    private final JournalDetailRepository journalDetailRepository;
    private final ChartOfAccountRepository chartOfAccountRepository;

    public Journal createJournal(String referenceType, Long referenceId, LocalDate date, String description) {
        Journal journal = new Journal();
        journal.setJournalDate(date != null ? date : LocalDate.now());
        journal.setReferenceType(referenceType);
        journal.setReferenceId(referenceId);
        journal.setDescription(description);
        journal.setStatus(JournalStatusEnum.POSTED);
        return journalRepository.save(journal);
    }

    /** Who a line's balance belongs to (e.g. PARTY_CUSTOMER, customerId). See JournalDetail.partyType/partyId. */
    public record Party(String type, Long id) {}

    /** Which original transaction a line traces back to (e.g. "SALE", saleId), independent of
     * which journal actually posted it. See JournalDetail.sourceType/sourceId. */
    public record Source(String type, Long id) {}

    public JournalDetail debit(Journal journal, ChartOfAccount account, BigDecimal amount, String description) {
        return debit(journal, account, amount, description, null, null);
    }

    public JournalDetail debit(Journal journal, ChartOfAccount account, BigDecimal amount, String description, Party party, Source source) {
        JournalDetail d = new JournalDetail();
        d.setJournal(journal);
        d.setAccount(account);
        d.setDebitAmount(amount);
        d.setCreditAmount(BigDecimal.ZERO);
        d.setDescription(description);
        applyTags(d, party, source);
        return d;
    }

    public JournalDetail credit(Journal journal, ChartOfAccount account, BigDecimal amount, String description) {
        return credit(journal, account, amount, description, null, null);
    }

    public JournalDetail credit(Journal journal, ChartOfAccount account, BigDecimal amount, String description, Party party, Source source) {
        JournalDetail c = new JournalDetail();
        c.setJournal(journal);
        c.setAccount(account);
        c.setDebitAmount(BigDecimal.ZERO);
        c.setCreditAmount(amount);
        c.setDescription(description);
        applyTags(c, party, source);
        return c;
    }

    private void applyTags(JournalDetail line, Party party, Source source) {
        if (party != null) {
            line.setPartyType(party.type());
            line.setPartyId(party.id());
        }
        if (source != null) {
            line.setSourceType(source.type());
            line.setSourceId(source.id());
        }
    }

    public void saveBalanced(List<JournalDetail> lines) {
        BigDecimal totalDebit = lines.stream().map(JournalDetail::getDebitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = lines.stream().map(JournalDetail::getCreditAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new BusinessException(ErrorCode.Business.JOURNAL_NOT_BALANCED);
        }
        journalDetailRepository.saveAll(lines);
    }

    public ChartOfAccount findAccountBySystemRole(String systemRole) {
        return chartOfAccountRepository.findBySystemRole(systemRole)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.JOURNAL_COA_MISSING));
    }

    public boolean isPosted(String referenceType, Long referenceId) {
        return journalRepository.findActiveByReferenceTypeAndReferenceId(referenceType, referenceId).isPresent();
    }

    @Transactional
    public void reverse(String referenceType, Long referenceId) {
        reverseOn(referenceType, referenceId, null);
    }

    @Transactional
    public void reverseOnDate(String referenceType, Long referenceId, LocalDate reversalDate) {
        reverseOn(referenceType, referenceId, reversalDate);
    }

    private void reverseOn(String referenceType, Long referenceId, LocalDate reversalDate) {
        Journal original = journalRepository.findActiveByReferenceTypeAndReferenceId(referenceType, referenceId)
                .orElse(null);
        if (original == null)
            return; // nothing to reverse

        List<JournalDetail> originalLines = journalDetailRepository.findByJournalId(original.getId());

        Journal reversal = new Journal();
        reversal.setJournalDate(reversalDate != null ? reversalDate : original.getJournalDate());
        reversal.setReferenceType(referenceType);
        reversal.setReferenceId(referenceId);
        reversal.setDescription("Reversal of: " + original.getDescription());
        reversal.setStatus(JournalStatusEnum.POSTED);
        reversal.setReversalOf(original);
        reversal = journalRepository.save(reversal);

        for (JournalDetail line : originalLines) {
            JournalDetail rev = new JournalDetail();
            rev.setJournal(reversal);
            rev.setAccount(line.getAccount());
            rev.setDebitAmount(line.getCreditAmount());   // swap
            rev.setCreditAmount(line.getDebitAmount());   // swap
            rev.setDescription("Reversal: " + (line.getDescription() != null ? line.getDescription() : ""));
            rev.setPartyType(line.getPartyType());
            rev.setPartyId(line.getPartyId());
            rev.setSourceType(line.getSourceType());
            rev.setSourceId(line.getSourceId());
            journalDetailRepository.save(rev);
        }

        original.setStatus(JournalStatusEnum.REVERSED);
        journalRepository.save(original);
    }

    @Transactional
    public Journal createManual(JournalRq rq) {
        if (rq.getLines() == null || rq.getLines().size() < 2) {
            throw new BusinessException("JNL_420", "Journal must have at least 2 lines");
        }
        BigDecimal totalDr = BigDecimal.ZERO;
        BigDecimal totalCr = BigDecimal.ZERO;
        for (var lineRq : rq.getLines()) {
            BigDecimal dr = safe(lineRq.getDebitAmount());
            BigDecimal cr = safe(lineRq.getCreditAmount());
            if (dr.signum() > 0 && cr.signum() > 0) {
                throw new BusinessException("JNL_421", "Each line must have debit OR credit, not both");
            }
            if (dr.signum() == 0 && cr.signum() == 0) {
                throw new BusinessException("JNL_422", "Each line must have a non-zero debit or credit");
            }
            if (lineRq.getAccountId() == null) {
                throw new BusinessException("JNL_423", "Each line must specify an accountId");
            }
            totalDr = totalDr.add(dr);
            totalCr = totalCr.add(cr);
        }
        if (totalDr.compareTo(totalCr) != 0) {
            throw new BusinessException(ErrorCode.Business.JOURNAL_NOT_BALANCED);
        }

        Journal journal = new Journal();
        journal.setJournalDate(rq.getJournalDate() != null ? rq.getJournalDate() : LocalDate.now());
        journal.setReferenceType("MANUAL_JOURNAL");
        journal.setReferenceId(0L); // placeholder, set to own id after save
        journal.setDescription(rq.getDescription() != null ? rq.getDescription() : "Manual journal entry");
        journal.setStatus(JournalStatusEnum.POSTED);
        journal = journalRepository.save(journal);
        journal.setReferenceId(journal.getId());
        journal = journalRepository.save(journal);

        List<JournalDetail> lines = new ArrayList<>();
        for (var lineRq : rq.getLines()) {
            ChartOfAccount account = chartOfAccountRepository.findById(lineRq.getAccountId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.Business.JOURNAL_COA_MISSING));
            JournalDetail line = new JournalDetail();
            line.setJournal(journal);
            line.setAccount(account);
            line.setDebitAmount(safe(lineRq.getDebitAmount()));
            line.setCreditAmount(safe(lineRq.getCreditAmount()));
            line.setDescription(lineRq.getDescription());
            lines.add(line);
        }
        journalDetailRepository.saveAll(lines);
        return journal;
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
