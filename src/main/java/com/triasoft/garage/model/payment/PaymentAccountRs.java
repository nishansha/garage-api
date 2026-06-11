package com.triasoft.garage.model.payment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.triasoft.garage.dto.PaymentAccountDTO;
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
public class PaymentAccountRs extends GenericRs {

    @Serial
    private static final long serialVersionUID = 6182930471592847361L;

    private Long id;
    private PaymentAccountDTO account;
    private List<PaymentAccountDTO> accounts;

}
