package com.triasoft.garage.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
public class WarehouseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 8213746192837465012L;

    private Long id;
    private Long version;
    private String code;
    private String name;
    private String address;
    private String location;

}
