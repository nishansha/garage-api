package com.triasoft.garage.company.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
public class CompanyDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long version;
    private String code;
    private String name;
    private String registrationNo;
    private String address;
    private boolean active;
}
