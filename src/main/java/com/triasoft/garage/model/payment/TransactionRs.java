package com.triasoft.garage.model.payment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.triasoft.garage.dto.TransactionDTO;
import com.triasoft.garage.model.common.GenericRs;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionRs extends GenericRs {

    @Serial
    private static final long serialVersionUID = 2938471650192837461L;

    private List<TransactionDTO> transactions;

}
