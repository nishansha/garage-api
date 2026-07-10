package com.triasoft.garage.service.impl;

import com.triasoft.garage.constants.AppConfig;
import com.triasoft.garage.constants.ClientChannel;
import com.triasoft.garage.entity.AppConfiguration;
import com.triasoft.garage.repository.AppConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppConfigurationService {

    private final AppConfigurationRepository appConfigurationRepository;


    public String getValue(AppConfig code, ClientChannel channel) {
        return appConfigurationRepository.findByCode(code.name())
                .map(config -> resolve(config, channel))
                .orElse(null);
    }


    public boolean isFlagEnabled(AppConfig code, ClientChannel channel) {
        return "Y".equalsIgnoreCase(getValue(code, channel));
    }

    private String resolve(AppConfiguration config, ClientChannel channel) {
        String value = null;
        if (channel == ClientChannel.WEB) {
            value = config.getWebValue();
        } else if (channel == ClientChannel.MOBILE) {
            value = config.getMobValue();
        }
        return (value == null || value.isBlank()) ? config.getGlobalValue() : value;
    }
}
