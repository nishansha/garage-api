package com.triasoft.garage.service.impl;

import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.dto.DirectEntryDTO;
import com.triasoft.garage.entity.ChartOfAccount;
import com.triasoft.garage.entity.DirectEntry;
import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.model.common.FilterRq;
import com.triasoft.garage.model.entry.DirectEntryRq;
import com.triasoft.garage.model.entry.DirectEntryRs;
import com.triasoft.garage.repository.ChartOfAccountRepository;
import com.triasoft.garage.repository.DirectEntryRepository;
import com.triasoft.garage.specifiction.DirectEntrySpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Other Income entries are Direct Entries whose Chart of Account is a REVENUE,
 * direct-postable account (e.g. Park & Sale, Commission, Insurance Commission,
 * Miscellaneous Income) — see journal_subsystem / direct_entry_module notes.
 * This service adds category scoping on top of DirectEntryService rather than
 * duplicating posting/reversal/journal logic, so existing Direct Entry rows
 * against these accounts surface here automatically.
 */
@Service
@RequiredArgsConstructor
public class OtherIncomeService {

    private static final String CATEGORY_TYPE = "REVENUE";

    private final DirectEntryRepository directEntryRepository;
    private final ChartOfAccountRepository chartOfAccountRepository;
    private final DirectEntryService directEntryService;

    public DirectEntryRs getAll(Pageable pageable) {
        Page<DirectEntry> page = directEntryRepository
                .findAllByChartOfAccount_TypeAndChartOfAccount_IsDirectPostableTrueOrderByEntryDateDescCreatedAtDesc(CATEGORY_TYPE, pageable);
        return toRs(page);
    }

    public DirectEntryRs search(FilterRq filter, Pageable pageable) {
        Page<DirectEntry> page = directEntryRepository.findAll(
                DirectEntrySpecification.buildOtherIncomeSearchQuery(filter), pageable);
        return toRs(page);
    }

    public DirectEntryDTO get(Long id) {
        return directEntryService.toDTO(findOtherIncome(id));
    }

    @Transactional
    public DirectEntryRs create(DirectEntryRq rq) {
        validateCategory(rq.getCoaId());
        return directEntryService.create(rq);
    }

    @Transactional
    public DirectEntryRs update(Long id, DirectEntryRq rq) {
        findOtherIncome(id);
        validateCategory(rq.getCoaId());
        return directEntryService.update(id, rq);
    }

    @Transactional
    public DirectEntryRs delete(Long id) {
        findOtherIncome(id);
        return directEntryService.delete(id);
    }

    private void validateCategory(Long coaId) {
        ChartOfAccount coa = chartOfAccountRepository.findById(coaId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.CHART_OF_ACCOUNT_NOT_FOUND));
        if (!CATEGORY_TYPE.equals(coa.getType()) || !coa.isDirectPostable()) {
            throw new BusinessException(ErrorCode.Business.OTHER_INCOME_INVALID_CATEGORY);
        }
    }

    private DirectEntry findOtherIncome(Long id) {
        DirectEntry entry = directEntryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.OTHER_INCOME_NOT_FOUND));
        ChartOfAccount coa = entry.getChartOfAccount();
        if (coa == null || !CATEGORY_TYPE.equals(coa.getType()) || !coa.isDirectPostable()) {
            throw new BusinessException(ErrorCode.Business.OTHER_INCOME_NOT_FOUND);
        }
        return entry;
    }

    private DirectEntryRs toRs(Page<DirectEntry> page) {
        DirectEntryRs rs = DirectEntryRs.builder()
                .entries(page.getContent().stream().map(directEntryService::toDTO).toList())
                .build();
        rs.setTotalPages(page.getTotalPages());
        rs.setTotalElements(page.getTotalElements());
        return rs;
    }

}
