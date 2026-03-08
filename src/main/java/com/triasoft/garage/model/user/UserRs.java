package com.triasoft.garage.model.user;

import com.triasoft.garage.dto.UserDTO;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
public class UserRs implements Serializable {
    @Serial
    private static final long serialVersionUID = 5504618272020838370L;
    private List<UserDTO> users;
}
