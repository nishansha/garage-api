package com.triasoft.garage.model.payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReconcileRq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<Long> transactionIds;

}
