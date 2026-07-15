package com.triasoft.garage.model.audit;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * A single field that changed between two revisions of an audited record.
 */
@Data
@AllArgsConstructor
public class FieldChange implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Object oldValue;
    private Object newValue;
}
