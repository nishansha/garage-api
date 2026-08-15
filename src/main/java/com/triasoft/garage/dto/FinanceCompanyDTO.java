package com.triasoft.garage.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
public class FinanceCompanyDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 3920184756123049183L;

    private Long id;
    private Long version;
    private String name;
    private String contactNumber;
    private String email;

}
