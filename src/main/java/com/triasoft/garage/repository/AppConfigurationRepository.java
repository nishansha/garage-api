package com.triasoft.garage.repository;

import com.triasoft.garage.entity.AppConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppConfigurationRepository extends JpaRepository<AppConfiguration, Long> {

    Optional<AppConfiguration> findByCode(String code);

}
