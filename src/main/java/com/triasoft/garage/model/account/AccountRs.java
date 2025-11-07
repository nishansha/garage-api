package com.triasoft.garage.model.account;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.triasoft.garage.dto.ChatOfAccountDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountRs implements Serializable {
    @Serial
    private static final long serialVersionUID = 2860783704546846343L;
    ChatOfAccountDTO account;
    List<ChatOfAccountDTO> accounts;
}
