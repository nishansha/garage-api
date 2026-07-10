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
@Table(name = "app_configurations")
public class AppConfiguration extends GenericEntity {

    @Serial
    private static final long serialVersionUID = 4863012984311457820L;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "global_value")
    private String globalValue;

    @Column(name = "web_value")
    private String webValue;

    @Column(name = "mob_value")
    private String mobValue;

}
