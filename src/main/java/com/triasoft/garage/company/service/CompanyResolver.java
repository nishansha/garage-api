package com.triasoft.garage.company.service;

import com.triasoft.garage.company.entity.Company;
import com.triasoft.garage.company.repository.CompanyRepository;
import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Resolves which company a request is scoped to. Callers pass an explicit companyId when they
 * have one (e.g. a client-supplied query param); when omitted, this defaults to the tenant's
 * sole company — the common case while a tenant has provisioned exactly one — and refuses to
 * guess once a tenant has more than one, forcing the caller to disambiguate explicitly.
 */
@Service
@RequiredArgsConstructor
public class CompanyResolver {

    private final CompanyRepository companyRepository;

    public Long resolveCompanyId(Long requestedCompanyId) {
        if (requestedCompanyId != null) {
            if (!companyRepository.existsById(requestedCompanyId)) {
                throw new BusinessException(ErrorCode.Business.COMPANY_NOT_FOUND);
            }
            return requestedCompanyId;
        }
        List<Company> companies = companyRepository.findAllByOrderByIdAsc();
        if (companies.isEmpty()) {
            throw new BusinessException(ErrorCode.Business.COMPANY_NOT_PROVISIONED);
        }
        if (companies.size() > 1) {
            throw new BusinessException(ErrorCode.Business.COMPANY_AMBIGUOUS);
        }
        return companies.get(0).getId();
    }

    /**
     * Same explicit-id validation as resolveCompanyId, but omitting companyId means "all
     * companies combined" (returns null) rather than refusing to guess. For reports that already
     * know how to aggregate across companies (see ReportService.getProfitAndLoss) once every
     * company-scoped term is null-aware — not a drop-in replacement for resolveCompanyId
     * elsewhere, since most callers (receivables/payables/etc.) scope single-company party
     * ledgers that have no "combined" meaning yet.
     */
    public Long resolveOptionalCompanyId(Long requestedCompanyId) {
        if (requestedCompanyId == null) {
            return null;
        }
        if (!companyRepository.existsById(requestedCompanyId)) {
            throw new BusinessException(ErrorCode.Business.COMPANY_NOT_FOUND);
        }
        return requestedCompanyId;
    }
}
