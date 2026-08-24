package com.triasoft.garage.service.impl;

import com.triasoft.garage.locking.VersionCheck;

import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.dto.ChatOfAccountDTO;
import com.triasoft.garage.dto.ExpenseDTO;
import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.ledger.entity.ChartOfAccount;
import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.model.account.AccountRq;
import com.triasoft.garage.model.account.AccountRs;
import com.triasoft.garage.ledger.repository.ChartOfAccountRepository;
import com.triasoft.garage.company.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AccountService {

    private static final Set<String> ALLOWED_TYPES = Set.of("ASSET", "LIABILITY", "EQUITY", "REVENUE", "EXPENSE");

    private final ChartOfAccountRepository chatOfAccountRepository;
    private final CompanyRepository companyRepository;

    public AccountRs getAccounts(AccountRq accountRq) {
        boolean filterByDirectPostable = Boolean.TRUE.equals(accountRq.getDirectPostable());
        boolean filterByType = StringUtils.hasLength(accountRq.getType());
        Long companyId = accountRq.getCompanyId();
        boolean filterByCompany = companyId != null;
        List<ChartOfAccount> accounts;
        if (filterByType && filterByDirectPostable) {
            accounts = filterByCompany
                    ? chatOfAccountRepository.findByTypeAndIsDirectPostableTrueAndCompanyId(accountRq.getType(), companyId)
                    : chatOfAccountRepository.findByTypeAndIsDirectPostableTrue(accountRq.getType());
        } else if (filterByDirectPostable) {
            accounts = filterByCompany
                    ? chatOfAccountRepository.findByIsDirectPostableTrueAndCompanyId(companyId)
                    : chatOfAccountRepository.findByIsDirectPostableTrue();
        } else if (filterByType) {
            accounts = filterByCompany
                    ? chatOfAccountRepository.findByTypeAndCompanyId(accountRq.getType(), companyId)
                    : chatOfAccountRepository.findByType(accountRq.getType());
        } else if (filterByCompany) {
            accounts = chatOfAccountRepository.findByCompanyId(companyId);
        } else {
            accounts = chatOfAccountRepository.findAll();
        }
        return AccountRs.builder().accounts(accounts.stream().map(this::toAccountDTO).toList()).build();
    }

    private ChatOfAccountDTO toAccountDTO(ChartOfAccount chartOfAccount) {
        ChatOfAccountDTO chatOfAccountDTO = new ChatOfAccountDTO();
        BeanUtils.copyProperties(chartOfAccount, chatOfAccountDTO);
        return chatOfAccountDTO;
    }

    public ChartOfAccount getOrCreateExpenseAccount(ExpenseDTO exDto, Long companyId, UserDTO user) {
        if (Objects.nonNull(exDto.getTypeId())) {
            return chatOfAccountRepository.findById(exDto.getTypeId()).orElseThrow(() -> new BusinessException(ErrorCode.Business.CHART_OF_ACCOUNT_NOT_FOUND));
        } else {
            return chatOfAccountRepository.findByTypeAndLabelIgnoreCaseAndCompanyId("EXPENSE", exDto.getTitle().trim(), companyId)
                    .orElseGet(() -> createChartOfAccount(ChatOfAccountDTO.builder()
                            .type("EXPENSE")
                            .label(exDto.getTitle())
                            .description(exDto.getDescription())
                            .build(), companyId, user));
        }
    }

    private ChartOfAccount createChartOfAccount(ChatOfAccountDTO accountDTO, Long companyId, UserDTO user) {
        Long lastInsertedCode = chatOfAccountRepository.findFirstByTypeAndCompanyIdOrderByCodeDesc(accountDTO.getType(), companyId)
                .map(c -> Long.parseLong(c.getCode()))
                .orElseGet(() -> getDefaultCodes(accountDTO.getType()));

        Long nextCode = ++lastInsertedCode;
        ChartOfAccount chartOfAccount = new ChartOfAccount();
        chartOfAccount.setCompanyId(companyId);
        chartOfAccount.setType(accountDTO.getType());
        chartOfAccount.setName(StringUtils.hasLength(accountDTO.getName()) ? accountDTO.getName() : (accountDTO.getType().charAt(0) + " - " + nextCode));
        chartOfAccount.setLabel(accountDTO.getLabel().trim());
        chartOfAccount.setCode(nextCode.toString());
        chartOfAccount.setDescription(StringUtils.hasLength(accountDTO.getDescription()) ? accountDTO.getDescription().trim() : null);
        chartOfAccount.setControlEnabled(false);
        chartOfAccount.setDirectPostable(accountDTO.isDirectPostable());
        return chatOfAccountRepository.save(chartOfAccount);
    }

    private void validateType(String type) {
        if (type == null || !ALLOWED_TYPES.contains(type.toUpperCase())) {
            throw new BusinessException(new ErrorCode.CustomError("COA_400",
                    "Type must be one of " + ALLOWED_TYPES));
        }
    }

    private Long getDefaultCodes(String type) {
        return switch (type.toUpperCase()) {
            case "ASSET" -> 1600L;
            case "LIABILITY" -> 2400L;
            case "EQUITY" -> 3300L;
            case "REVENUE" -> 4300L;
            case "EXPENSE" -> 6800L;
            default -> 100L;
        };
    }

    public AccountRs create(AccountRq accountRq, UserDTO user) {
        validateType(accountRq.getType());
        if (!companyRepository.existsById(accountRq.getCompanyId())) {
            throw new BusinessException(ErrorCode.Business.COMPANY_NOT_FOUND);
        }
        ChartOfAccount chartOfAccount = chatOfAccountRepository.findByTypeAndLabelIgnoreCaseAndCompanyId(accountRq.getType(), accountRq.getLabel(), accountRq.getCompanyId()).orElse(null);
        if (Objects.nonNull(chartOfAccount)) throw new BusinessException(ErrorCode.Business.CHART_OF_ACCOUNT_EXIST);

        ChatOfAccountDTO accountDTO = new ChatOfAccountDTO();
        BeanUtils.copyProperties(accountRq, accountDTO);
        ChartOfAccount newAccount = createChartOfAccount(accountDTO, accountRq.getCompanyId(), user);
        return AccountRs.builder().account(this.toAccountDTO(newAccount)).build();
    }

    public ChatOfAccountDTO get(Long id, UserDTO user) {
        ChartOfAccount chartOfAccount = chatOfAccountRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.Business.CHART_OF_ACCOUNT_NOT_FOUND));
        return toAccountDTO(chartOfAccount);
    }

    @VersionCheck(entity = ChartOfAccount.class)
    public AccountRs update(Long id, AccountRq accountRq, UserDTO user) {
        validateType(accountRq.getType());
        ChartOfAccount chartOfAccount = chatOfAccountRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.Business.CHART_OF_ACCOUNT_NOT_FOUND));
        // Reject renaming this account to a (type, label) already used by another account in the same company.
        // companyId itself is immutable post-creation (not editable via this endpoint), same as PaymentAccountRq.
        chatOfAccountRepository.findByTypeAndLabelIgnoreCaseAndIdNotAndCompanyId(accountRq.getType(), accountRq.getLabel(), id, chartOfAccount.getCompanyId())
                .ifPresent(existing -> { throw new BusinessException(ErrorCode.Business.CHART_OF_ACCOUNT_EXIST); });
        chartOfAccount.setLabel(accountRq.getLabel());
        chartOfAccount.setDescription(accountRq.getDescription());
        if (!chartOfAccount.getType().equalsIgnoreCase(accountRq.getType())) {
            Long lastInsertedCode = chatOfAccountRepository.findFirstByTypeAndCompanyIdOrderByCodeDesc(accountRq.getType(), chartOfAccount.getCompanyId())
                    .map(c -> Long.parseLong(c.getCode()))
                    .orElseGet(() -> getDefaultCodes(accountRq.getType()));
            Long nextCode = ++lastInsertedCode;
            chartOfAccount.setType(accountRq.getType());
            chartOfAccount.setName(StringUtils.hasLength(accountRq.getName()) ? accountRq.getName() : (accountRq.getType().charAt(0) + " - " + nextCode));
            chartOfAccount.setCode(nextCode.toString());
        }
        chartOfAccount = chatOfAccountRepository.save(chartOfAccount);
        return AccountRs.builder().account(this.toAccountDTO(chartOfAccount)).build();
    }

    public AccountRs delete(Long id, UserDTO user) {
        ChartOfAccount chartOfAccount = chatOfAccountRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.Business.CHART_OF_ACCOUNT_NOT_FOUND));
        chatOfAccountRepository.delete(chartOfAccount);
        return AccountRs.builder().build();
    }
}
