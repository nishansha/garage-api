package com.triasoft.garage.service;

import com.triasoft.garage.dto.ChatOfAccountDTO;
import com.triasoft.garage.entity.ChatOfAccount;
import com.triasoft.garage.model.account.AccountRq;
import com.triasoft.garage.model.account.AccountRs;
import com.triasoft.garage.repository.ChatOfAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final ChatOfAccountRepository chatOfAccountRepository;

    public AccountRs getAccounts(AccountRq accountRq) {
        if (StringUtils.hasLength(accountRq.getType())) {
            List<ChatOfAccount> accounts = chatOfAccountRepository.findByType(accountRq.getType());
            return AccountRs.builder().accounts(accounts.stream().map(this::toAccountDTO).toList()).build();
        }
        List<ChatOfAccount> accounts = chatOfAccountRepository.findAll();
        return AccountRs.builder().accounts(accounts.stream().map(this::toAccountDTO).toList()).build();
    }

    private ChatOfAccountDTO toAccountDTO(ChatOfAccount chatOfAccount) {
        ChatOfAccountDTO chatOfAccountDTO = new ChatOfAccountDTO();
        BeanUtils.copyProperties(chatOfAccount, chatOfAccountDTO);
        return chatOfAccountDTO;
    }
}
