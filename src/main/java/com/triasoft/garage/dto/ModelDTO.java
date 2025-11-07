package com.triasoft.garage.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class ModelDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = -3679765402852945536L;
    private Long id;
    private String code;
    private String description;
}