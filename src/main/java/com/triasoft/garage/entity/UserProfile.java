package com.triasoft.garage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serial;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "user_profile")
public class UserProfile extends GenericEntity {

    @Serial
    private static final long serialVersionUID = -7124710920831301261L;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "password",nullable = false)
    private String password;
}
