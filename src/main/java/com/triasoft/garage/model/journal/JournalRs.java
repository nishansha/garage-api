package com.triasoft.garage.model.journal;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
public class JournalRs implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String referenceType;
    private Long referenceId;

}
