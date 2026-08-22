package com.triasoft.garage.company.service;

import com.triasoft.garage.company.dto.CompanyDTO;
import com.triasoft.garage.company.entity.Company;
import com.triasoft.garage.company.entity.UserCompanyAccess;
import com.triasoft.garage.company.model.CompanyRq;
import com.triasoft.garage.company.model.CompanyRs;
import com.triasoft.garage.company.repository.CompanyRepository;
import com.triasoft.garage.company.repository.UserCompanyAccessRepository;
import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.constants.SystemCoaRole;
import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.ledger.entity.ChartOfAccount;
import com.triasoft.garage.ledger.repository.ChartOfAccountRepository;
import com.triasoft.garage.locking.VersionCheck;
import com.triasoft.garage.repository.UserProfileRepository;
import com.triasoft.garage.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final UserCompanyAccessRepository userCompanyAccessRepository;
    private final UserProfileRepository userProfileRepository;
    private final WarehouseRepository warehouseRepository;
    private final ChartOfAccountRepository chartOfAccountRepository;

    // Mirrors scripts/provision-company-template.sql's CoA seed rows (kept in sync manually),
    // plus RC_DUE_RECEIVABLE which that script's own comment flags as a known-missing row —
    // fixed here since PurchaseService/JournalService rely on it for RC due postings.
    private static final List<CoaSeed> SYSTEM_COA_SEED = List.of(
            new CoaSeed("1100", "A-ACR", "Money owed to the company by customers. (Control Account)", "ASSET", true, "Accounts Receivable (A/R)", false, SystemCoaRole.AR),
            new CoaSeed("1150", "A-FR", "Amount due from finance companies for financed vehicle sales.", "ASSET", false, "Finance Receivable", false, SystemCoaRole.FINANCE_RECEIVABLE),
            new CoaSeed("1170", "VND-REF-REC", "Vendor Refund Receivable", "ASSET", true, "Money owed to us by vendors for returned purchases", false, SystemCoaRole.VENDOR_REFUND_RECEIVABLE),
            new CoaSeed("1180", "A-RCD", "Amount due from the vehicle's original purchase vendor for RCD splits, collected at RC conversion", "ASSET", false, "RC Due Receivable", false, SystemCoaRole.RC_DUE_RECEIVABLE),
            new CoaSeed("1200", "A-INV", "Total cost of all vehicles in stock. (Control Account)", "ASSET", true, "Inventory - Vehicles", false, SystemCoaRole.INVENTORY),
            new CoaSeed("2000", "L-ACP", "Money owed by the company to vendors/suppliers. (Control Account)", "LIABILITY", true, "Accounts Payable (A/P)", false, SystemCoaRole.AP),
            new CoaSeed("2400", "L-CSP", "Amounts owed to customers from trade-in exchanges where exchange value exceeds sale value.", "LIABILITY", false, "Customer Settlement Payable", false, SystemCoaRole.CUSTOMER_SETTLEMENT_PAYABLE),
            new CoaSeed("2410", "CUST-REF-PAY", "Customer Refund Payable", "LIABILITY", true, "Money owed to customers for returned sales, pending refund", false, SystemCoaRole.CUSTOMER_REFUND_PAYABLE),
            new CoaSeed("3900", "E-OBE", "Temporary clearing account for opening balances during initial setup.", "EQUITY", false, "Opening Balance Equity", false, SystemCoaRole.OPENING_BALANCE_EQUITY),
            new CoaSeed("4000", "R-IN", "Gross revenue from the sale of new and used vehicles.", "REVENUE", false, "Sales Revenue - Vehicles", false, SystemCoaRole.SALES_REVENUE),
            new CoaSeed("4010", "R-SVC", "Revenue from standalone services (e.g. car wash) not tied to a vehicle sale.", "REVENUE", false, "Service Revenue", false, SystemCoaRole.SERVICE_REVENUE),
            new CoaSeed("4520", "RTN-INC-DED", "Return Deduction Income", "REVENUE", true, "Return Deduction Income", false, SystemCoaRole.RETURN_DEDUCTION_INCOME),
            new CoaSeed("4530", "RTN-INC-EXGAIN", "Gain on Exchange Adjustment", "REVENUE", true, "Gain on Exchange Adjustment", false, SystemCoaRole.GAIN_ON_EXCHANGE_ADJ),
            new CoaSeed("5000", "CGOS-IN", "The landed cost of vehicles that have been sold.", "EXPENSE", false, "COGS - Vehicles", false, SystemCoaRole.COGS),
            new CoaSeed("5100", "E-SAL", "Salaries paid to employees.", "EXPENSE", false, "Salary Expense", false, SystemCoaRole.SALARY_EXPENSE),
            new CoaSeed("5510", "RTN-EXP-EXCH", "Loss on Returned Exchange Vehicle", "EXPENSE", true, "Loss on Returned Exchange Vehicle", false, SystemCoaRole.LOSS_RETURNED_EXCHANGE),
            new CoaSeed("5520", "RTN-EXP-PUR", "Loss on Purchase Return", "EXPENSE", true, "Loss on Purchase Return", false, SystemCoaRole.LOSS_PURCHASE_RETURN)
    );

    private record CoaSeed(String code, String name, String description, String type, boolean isControlEnabled,
                            String label, boolean isDirectPostable, SystemCoaRole systemRole) {
    }

    public CompanyRs getAll() {
        List<CompanyDTO> companies = companyRepository.findAllByOrderByIdAsc().stream()
                .map(this::toDTO)
                .toList();
        return CompanyRs.builder().companies(companies).build();
    }

    public CompanyDTO get(Long id) {
        return toDTO(findById(id));
    }

    @Transactional
    public CompanyRs create(CompanyRq rq) {
        String code = rq.getCode().trim().toUpperCase();
        if (companyRepository.findAllByOrderByIdAsc().stream().anyMatch(c -> c.getCode().equalsIgnoreCase(code))) {
            throw new BusinessException(ErrorCode.Business.COMPANY_CODE_EXISTS);
        }
        Company company = new Company();
        company.setCode(code);
        company.setName(rq.getName());
        company.setRegistrationNo(rq.getRegistrationNo());
        company.setAddress(rq.getAddress());
        company.setActive(rq.isActive());
        companyRepository.save(company);
        seedChartOfAccounts(company.getId());
        return CompanyRs.builder().id(company.getId()).build();
    }

    private void seedChartOfAccounts(Long companyId) {
        List<ChartOfAccount> accounts = SYSTEM_COA_SEED.stream()
                .map(seed -> {
                    ChartOfAccount account = new ChartOfAccount();
                    account.setCompanyId(companyId);
                    account.setCode(seed.code());
                    account.setName(seed.name());
                    account.setDescription(seed.description());
                    account.setType(seed.type());
                    account.setControlEnabled(seed.isControlEnabled());
                    account.setLabel(seed.label());
                    account.setDirectPostable(seed.isDirectPostable());
                    account.setSystemRole(seed.systemRole().name());
                    return account;
                })
                .toList();
        chartOfAccountRepository.saveAll(accounts);
    }

    @Transactional
    @VersionCheck(entity = Company.class)
    public CompanyRs update(Long id, CompanyRq rq) {
        Company company = findById(id);
        String code = rq.getCode().trim().toUpperCase();
        if (companyRepository.findAllByOrderByIdAsc().stream()
                .anyMatch(c -> !c.getId().equals(id) && c.getCode().equalsIgnoreCase(code))) {
            throw new BusinessException(ErrorCode.Business.COMPANY_CODE_EXISTS);
        }
        company.setCode(code);
        company.setName(rq.getName());
        company.setRegistrationNo(rq.getRegistrationNo());
        company.setAddress(rq.getAddress());
        company.setActive(rq.isActive());
        companyRepository.save(company);
        return CompanyRs.builder().id(company.getId()).build();
    }

    @Transactional
    public void delete(Long id) {
        Company company = findById(id);
        if (warehouseRepository.existsByCompanyId(id)) {
            throw new BusinessException(ErrorCode.Business.COMPANY_IN_USE);
        }
        userCompanyAccessRepository.findByCompanyId(id).forEach(userCompanyAccessRepository::delete);
        companyRepository.delete(company);
    }

    @Transactional
    public void grantAccess(Long companyId, Long userId) {
        findById(companyId);
        if (!userProfileRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.Business.USER_NOT_FOUND);
        }
        if (userCompanyAccessRepository.existsByUserIdAndCompanyId(userId, companyId)) {
            return; // idempotent
        }
        UserCompanyAccess access = new UserCompanyAccess();
        access.setUserId(userId);
        access.setCompanyId(companyId);
        userCompanyAccessRepository.save(access);
    }

    @Transactional
    public void revokeAccess(Long companyId, Long userId) {
        userCompanyAccessRepository.findByUserIdAndCompanyId(userId, companyId)
                .ifPresent(userCompanyAccessRepository::delete);
    }

    public List<Long> getGrantedUserIds(Long companyId) {
        return userCompanyAccessRepository.findByCompanyId(companyId).stream()
                .map(UserCompanyAccess::getUserId)
                .toList();
    }

    public List<Long> getAccessibleCompanyIds(Long userId) {
        return userCompanyAccessRepository.findByUserId(userId).stream()
                .map(UserCompanyAccess::getCompanyId)
                .toList();
    }

    private Company findById(Long id) {
        return companyRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.Business.COMPANY_NOT_FOUND));
    }

    private CompanyDTO toDTO(Company company) {
        return CompanyDTO.builder()
                .id(company.getId())
                .version(company.getVersion())
                .code(company.getCode())
                .name(company.getName())
                .registrationNo(company.getRegistrationNo())
                .address(company.getAddress())
                .active(company.isActive())
                .build();
    }
}
