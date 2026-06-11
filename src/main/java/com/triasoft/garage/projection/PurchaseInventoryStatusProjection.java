package com.triasoft.garage.projection;

import com.triasoft.garage.constants.StatusEnum;

public interface PurchaseInventoryStatusProjection {
    Long getPurchaseId();
    StatusEnum getStatus();
}
