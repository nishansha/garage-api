package com.triasoft.garage.service.impl;

import com.triasoft.garage.locking.VersionCheck;

import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.dto.ChatOfAccountDTO;
import com.triasoft.garage.dto.ExpenseDTO;
import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.entity.ChartOfAccount;
import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.model.account.AccountRq;
import com.triasoft.garage.model.account.AccountRs;
import com.triasoft.garage.repository.ChartOfAccountRepository;
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

    public AccountRs getAccounts(AccountRq accountRq) {
        boolean filterByDirectPostable = Boolean.TRUE.equals(accountRq.getDirectPostable());
        boolean filterByType = StringUtils.hasLength(accountRq.getType());
        List<ChartOfAccount> accounts;
        if (filterByType && filterByDirectPostable) {
            accounts = chatOfAccountRepository.findByTypeAndIsDirectPostableTrue(accountRq.getType());
        } else if (filterByDirectPostable) {
            accounts = chatOfAccountRepository.findByIsDirectPostableTrue();
        } else if (filterByType) {
            accounts = chatOfAccountRepository.findByType(accountRq.getType());
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

    public ChartOfAccount getOrCreateExpenseAccount(ExpenseDTO exDto, UserDTO user) {
        if (Objects.nonNull(exDto.getTypeId())) {
            return chatOfAccountRepository.findById(exDto.getTypeId()).orElseThrow(() -> new BusinessException(ErrorCode.Business.CHART_OF_ACCOUNT_NOT_FOUND));
        } else {
            return chatOfAccountRepository.findByTypeAndLabelIgnoreCase("EXPENSE", exDto.getTitle().trim())
                    .orElseGet(() -> createChartOfAccount(ChatOfAccountDTO.builder()
                            .type("EXPENSE")
                            .label(exDto.getTitle())
                            .description(exDto.getDescription())
                            .build(), user));
        }
    }

    private ChartOfAccount createChartOfAccount(ChatOfAccountDTO accountDTO, UserDTO user) {
        Long lastInsertedCode = chatOfAccountRepository.findFirstByTypeOrderByCodeDesc(accountDTO.getType())
                .map(c -> Long.parseLong(c.getCode()))
                .orElseGet(() -> getDefaultCodes(accountDTO.getType()));

        Long nextCode = ++lastInsertedCode;
        ChartOfAccount chartOfAccount = new ChartOfAccount();
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
        ChartOfAccount chartOfAccount = chatOfAccountRepository.findByTypeAndLabelIgnoreCase(accountRq.getType(), accountRq.getLabel()).orElse(null);
        if (Objects.nonNull(chartOfAccount)) throw new BusinessException(ErrorCode.Business.CHART_OF_ACCOUNT_EXIST);

        ChatOfAccountDTO accountDTO = new ChatOfAccountDTO();
        BeanUtils.copyProperties(accountRq, accountDTO);
        ChartOfAccount newAccount = createChartOfAccount(accountDTO, user);
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
        // Reject renaming this account to a (type, label) already used by another account
        chatOfAccountRepository.findByTypeAndLabelIgnoreCaseAndIdNot(accountRq.getType(), accountRq.getLabel(), id)
                .ifPresent(existing -> { throw new BusinessException(ErrorCode.Business.CHART_OF_ACCOUNT_EXIST); });
        chartOfAccount.setLabel(accountRq.getLabel());
        chartOfAccount.setDescription(accountRq.getDescription());
        if (!chartOfAccount.getType().equalsIgnoreCase(accountRq.getType())) {
            Long lastInsertedCode = chatOfAccountRepository.findFirstByTypeOrderByCodeDesc(accountRq.getType())
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
