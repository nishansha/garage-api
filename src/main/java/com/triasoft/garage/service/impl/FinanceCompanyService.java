package com.triasoft.garage.service.impl;

import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.dto.FinanceCompanyDTO;
import com.triasoft.garage.entity.FinanceCompany;
import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.locking.VersionCheck;
import com.triasoft.garage.model.finance.FinanceCompanyRq;
import com.triasoft.garage.model.finance.FinanceCompanyRs;
import com.triasoft.garage.repository.FinanceCompanyRepository;
import com.triasoft.garage.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FinanceCompanyService {

    private final FinanceCompanyRepository financeCompanyRepository;
    private final SaleRepository saleRepository;

    public FinanceCompanyRs getAll() {
        List<FinanceCompanyDTO> financeCompanies = financeCompanyRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
        return FinanceCompanyRs.builder().financeCompanies(financeCompanies).build();
    }

    public FinanceCompanyDTO get(Long id) {
        return toDTO(findById(id));
    }

    @Transactional
    public FinanceCompanyRs create(FinanceCompanyRq rq) {
        String name = rq.getName().trim();
        if (financeCompanyRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException(ErrorCode.Business.FINANCE_COMPANY_NAME_EXISTS);
        }
        FinanceCompany financeCompany = new FinanceCompany();
        financeCompany.setName(name);
        financeCompany.setContactNumber(rq.getContactNumber());
        financeCompany.setEmail(rq.getEmail());
        financeCompanyRepository.save(financeCompany);
        return FinanceCompanyRs.builder().id(financeCompany.getId()).build();
    }

    @Transactional
    @VersionCheck(entity = FinanceCompany.class)
    public FinanceCompanyRs update(Long id, FinanceCompanyRq rq) {
        FinanceCompany financeCompany = findById(id);
        String name = rq.getName().trim();
        if (financeCompanyRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BusinessException(ErrorCode.Business.FINANCE_COMPANY_NAME_EXISTS);
        }
        financeCompany.setName(name);
        financeCompany.setContactNumber(rq.getContactNumber());
        financeCompany.setEmail(rq.getEmail());
        financeCompanyRepository.save(financeCompany);
        return FinanceCompanyRs.builder().id(financeCompany.getId()).build();
    }

    @Transactional
    public void delete(Long id) {
        FinanceCompany financeCompany = findById(id);
        if (saleRepository.existsByFinanceCompanyRefId(id)) {
            throw new BusinessException(ErrorCode.Business.FINANCE_COMPANY_IN_USE);
        }
        financeCompanyRepository.delete(financeCompany);
    }

    private FinanceCompany findById(Long id) {
        return financeCompanyRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.Business.FINANCE_COMPANY_NOT_FOUND));
    }

    private FinanceCompanyDTO toDTO(FinanceCompany financeCompany) {
        return FinanceCompanyDTO.builder()
                .id(financeCompany.getId())
                .version(financeCompany.getVersion())
                .name(financeCompany.getName())
                .contactNumber(financeCompany.getContactNumber())
                .email(financeCompany.getEmail())
                .build();
    }
}
