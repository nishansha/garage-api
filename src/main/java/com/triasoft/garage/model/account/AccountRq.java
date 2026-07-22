package com.triasoft.garage.model.account;

import com.triasoft.garage.locking.Versioned;
import com.triasoft.garage.validation.NullOrNotBlank;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class AccountRq implements Serializable, Versioned {
    @Serial
    private static final long serialVersionUID = -4425259029479130460L;

    private Long id;
    private Long version;

    @NotBlank(message = "REQUIRED")
    private String type;

    private String code;

    @Size(max = 100, message = "MAX_LENGTH")
    @NullOrNotBlank
    private String name;

    @NotBlank(message = "REQUIRED")
    @Size(max = 100, message = "MAX_LENGTH")
    private String label;

    @Size(max = 255, message = "MAX_LENGTH")
    @NullOrNotBlank
    private String description;

    private Boolean directPostable;

}
