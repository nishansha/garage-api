package com.triasoft.garage.model.common;

import lombok.Data;
import org.springframework.data.domain.Pageable;

import java.io.Serial;
import java.io.Serializable;

@Data
public class GenericRq implements Serializable {
    @Serial
    private static final long serialVersionUID = -4062110970884623835L;
    private Long userId;
    private Pageable pageable;
}
