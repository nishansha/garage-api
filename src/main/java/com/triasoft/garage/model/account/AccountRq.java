package com.triasoft.garage.model.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountRq implements Serializable {
    @Serial
    private static final long serialVersionUID = -4425259029479130460L;
    private Long id;
    private String type;
    private String code;
    private String name;
    private String label;
    private String description;

}
