package com.triasoft.garage.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class VarientDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = -2517398167907856011L;
    private Long id;
    private String code;
    private String description;
}
