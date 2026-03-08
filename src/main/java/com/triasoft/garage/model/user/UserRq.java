package com.triasoft.garage.model.user;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
public class UserRq implements Serializable {
    @Serial
    private static final long serialVersionUID = -5448314308032610312L;
    private String userName;
    private String password;
    private String name;
}
