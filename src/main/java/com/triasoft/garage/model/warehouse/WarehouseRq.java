package com.triasoft.garage.model.warehouse;

import com.triasoft.garage.locking.Versioned;
import com.triasoft.garage.validation.NullOrNotBlank;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class WarehouseRq implements Serializable, Versioned {

    @Serial
    private static final long serialVersionUID = -6172836451920384756L;

    private Long version;

    @NotBlank(message = "REQUIRED")
    @Size(max = 50, message = "MAX_LENGTH")
    private String code;

    @NotBlank(message = "REQUIRED")
    @Size(max = 255, message = "MAX_LENGTH")
    private String name;

    @Size(max = 255, message = "MAX_LENGTH")
    @NullOrNotBlank
    private String address;

    @NullOrNotBlank
    private String location;

}
