package com.triasoft.garage.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
public class DataInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = 8966215999223383984L;
    private String name;
    private String value;
}
