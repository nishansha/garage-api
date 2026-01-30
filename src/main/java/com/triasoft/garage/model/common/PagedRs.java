package com.triasoft.garage.model.common;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class PagedRs implements Serializable {
    @Serial
    private static final long serialVersionUID = 1009241973759630758L;
    private int totalPages;
    private Long totalElements;
}
