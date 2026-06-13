package com.triasoft.garage.service.impl;

import com.triasoft.garage.constants.*;
import com.triasoft.garage.dto.DirectEntryDTO;
import com.triasoft.garage.entity.DirectEntry;
import com.triasoft.garage.entity.LookupMaster;
import com.triasoft.garage.entity.PaymentAccount;
import com.triasoft.garage.entity.Transaction;
import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.helper.LookupHelper;
import com.triasoft.garage.model.common.FilterRq;
import com.triasoft.garage.model.entry.DirectEntryRq;
import com.triasoft.garage.model.entry.DirectEntryRs;
import com.triasoft.garage.repository.DirectEntryRepository;
import com.triasoft.garage.repository.LookupMasterRepository;
import com.triasoft.garage.repository.PaymentAccountRepository;
import com.triasoft.garage.repository.TransactionRepository;
import com.triasoft.garage.specifiction.DirectEntrySpecification;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DirectEntryService {

    private static final String REFERENCE_TYPE = "DIRECT_ENTRY";

    private final DirectEntryRepository directEntryRepository;
    private final PaymentAccountRepository paymentAccountRepository;
    private final TransactionRepository transactionRepository;
    private final LookupMasterRepository lookupMasterRepository;

    public DirectEntryRs getAll(Pageable pageable) {
        Page<DirectEntry> page = directEntryRepository.findAllByOrderByEntryDateDescCreatedAtDesc(pageable);
        DirectEntryRs rs = DirectEntryRs.builder()
                .entries(page.getContent().stream().map(this::toDTO).toList())
                .build();
        rs.setTotalPages(page.getTotalPages());
        rs.setTotalElements(page.getTotalElements());
        return rs;
    }

    public DirectEntryRs search(FilterRq filter, Pageable pageable) {
        Page<DirectEntry> page = directEntryRepository.findAll(
                DirectEntrySpecification.buildSearchQuery(filter), pageable);
        DirectEntryRs rs = DirectEntryRs.builder()
                .entries(page.getContent().stream().map(this::toDTO).toList())
                .build();
        rs.setTotalPages(page.getTotalPages());
        rs.setTotalElements(page.getTotalElements());
        return rs;
    }

    public DirectEntryDTO get(Long id) {
        return toDTO(find(id));
    }

    @Transactional
    public DirectEntryRs create(DirectEntryRq rq) {
        validate(rq);
        DirectEntry entry = new DirectEntry();
        mapFields(entry, rq);
        entry = directEntryRepository.save(entry);
        postTransaction(entry);
        // TODO [JOURNAL ENTRY] - Direct Entry
        // direction=IN:  Dr <paymentAccount> (Asset) / Cr account per type (Equity/Income/Liability)
        // direction=OUT: Dr account per type (Equity/Expense) / Cr <paymentAccount> (Asset)
        // The CoA counterpart account will be configurable per DirectEntryType in the lookup.
        // Future call: JournalEntryService.postDirectEntry(entry.getId())
        return DirectEntryRs.builder().id(entry.getId()).build();
    }

    @Transactional
    public DirectEntryRs update(Long id, DirectEntryRq rq) {
        validate(rq);
        DirectEntry entry = find(id);
        reverseTransaction(entry);
        mapFields(entry, rq);
        entry = directEntryRepository.save(entry);
        postTransaction(entry);
        return DirectEntryRs.builder().id(entry.getId()).build();
    }

    @Transactional
    public DirectEntryRs delete(Long id) {
        DirectEntry entry = find(id);
        reverseTransaction(entry);
        directEntryRepository.delete(entry);
        return DirectEntryRs.builder().build();
    }

    private void validate(DirectEntryRq rq) {
        if (rq.getTypeId() == null) {
            throw new BusinessException("DE_400", "Entry type is required");
        }
        if (rq.getDirection() == null) {
            throw new BusinessException("DE_401", "Direction (IN/OUT) is required");
        }
        if (rq.getAmount() == null || rq.getAmount().signum() <= 0) {
            throw new BusinessException("DE_402", "Amount must be greater than zero");
        }
        if (rq.getPaymentAccountId() == null) {
            throw new BusinessException("DE_403", "Payment account is required");
        }
    }

    private void mapFields(DirectEntry entry, DirectEntryRq rq) {
        LookupMaster type = lookupMasterRepository.findById(rq.getTypeId())
                .orElseThrow(() -> new BusinessException("DE_404", "Entry type not found"));
        PaymentAccount account = paymentAccountRepository.findById(rq.getPaymentAccountId())
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.PAYMENT_ACCOUNT_NOT_FOUND));
        entry.setEntryDate(rq.getEntryDate() != null ? rq.getEntryDate() : LocalDate.now());
        entry.setType(type);
        entry.setDirection(rq.getDirection());
        entry.setAmount(rq.getAmount());
        entry.setPaymentAccount(account);
        entry.setPartyName(rq.getPartyName());
        entry.setReferenceNo(rq.getReferenceNo());
        entry.setDescription(rq.getDescription());
        entry.setNotes(rq.getNotes());
    }

    private void postTransaction(DirectEntry entry) {
        Transaction txn = new Transaction();
        txn.setTransactionDate(entry.getEntryDate());
        txn.setType(TransactionTypeEnum.DIRECT_ENTRY);
        txn.setReferenceType(REFERENCE_TYPE);
        txn.setReferenceId(entry.getId());
        txn.setPaymentAccount(entry.getPaymentAccount());
        txn.setAmount(entry.getAmount());
        txn.setDirection(entry.getDirection());
        txn.setDescription(buildDescription(entry));
        txn.setNotes(entry.getNotes());
        transactionRepository.save(txn);
    }

    private void reverseTransaction(DirectEntry entry) {
        transactionRepository.findActiveByReferenceTypeAndReferenceId(REFERENCE_TYPE, entry.getId())
                .ifPresent(original -> {
                    if (transactionRepository.existsByReversalOfId(original.getId())) {
                        throw new BusinessException(ErrorCode.Business.DIRECT_ENTRY_ALREADY_REVERSED);
                    }
                    Transaction reversal = new Transaction();
                    reversal.setTransactionDate(LocalDate.now());
                    reversal.setType(TransactionTypeEnum.DIRECT_ENTRY);
                    reversal.setReferenceType(REFERENCE_TYPE);
                    reversal.setReferenceId(entry.getId());
                    reversal.setPaymentAccount(original.getPaymentAccount());
                    reversal.setAmount(original.getAmount());
                    reversal.setDirection(TransactionDirectionEnum.IN.equals(original.getDirection())
                            ? TransactionDirectionEnum.OUT : TransactionDirectionEnum.IN);
                    reversal.setDescription("Reversal – " + original.getDescription());
                    reversal.setReversalOf(original);
                    transactionRepository.save(reversal);
                });
    }

    private String buildDescription(DirectEntry entry) {
        String typeName = entry.getType().getDescription();
        return entry.getPartyName() != null
                ? typeName + " – " + entry.getPartyName()
                : typeName;
    }

    private DirectEntry find(Long id) {
        return directEntryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.Business.DIRECT_ENTRY_NOT_FOUND.getMessage()));
    }

    private DirectEntryDTO toDTO(DirectEntry e) {
        return DirectEntryDTO.builder()
                .id(e.getId())
                .entryDate(e.getEntryDate())
                .typeId(e.getType().getId())
                .typeCode(e.getType().getCode())
                .typeName(e.getType().getDescription())
                .direction(e.getDirection())
                .amount(e.getAmount())
                .paymentAccountId(e.getPaymentAccount() != null ? e.getPaymentAccount().getId() : null)
                .paymentAccountName(e.getPaymentAccount() != null ? e.getPaymentAccount().getName() : null)
                .partyName(e.getPartyName())
                .referenceNo(e.getReferenceNo())
                .description(e.getDescription())
                .notes(e.getNotes())
                .createdAt(e.getCreatedAt())
                .build();
    }

}
