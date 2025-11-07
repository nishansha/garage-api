package com.triasoft.garage.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class ChatOfAccountDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = -5413015889738105503L;
    private Long id;
    private String type;
    private String name;
    private String code;
    private String label;
    private String description;
    private boolean isControlEnabled;
}
