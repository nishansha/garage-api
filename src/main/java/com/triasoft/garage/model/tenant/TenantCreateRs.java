package com.triasoft.garage.model.tenant;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
public class TenantCreateRs implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long tenantId;
    private Long userId;
}
