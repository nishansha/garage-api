package com.triasoft.garage.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class BrandDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = -2729994720135375827L;
    private Long id;
    private String code;
    private String description;
}