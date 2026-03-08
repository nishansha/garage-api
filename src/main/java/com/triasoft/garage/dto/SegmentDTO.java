package com.triasoft.garage.dto;

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
public class SegmentDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 8803003533442904025L;
    private Long id;
    private String code;
    private String description;
}
