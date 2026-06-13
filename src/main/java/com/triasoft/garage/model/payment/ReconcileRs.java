package com.triasoft.garage.model.payment;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
public class ReconcileRs implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int totalRequested;
    private int reconciled;
    private int alreadyReconciled;
    private int skipped;

}
