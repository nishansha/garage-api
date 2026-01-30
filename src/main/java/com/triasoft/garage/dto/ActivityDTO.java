package com.triasoft.garage.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
public class ActivityDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1063613538429775711L;
    private String activityType;
    private String description;
    private String dateTime;
    private String txnType;
    private String txnAmount;
}
