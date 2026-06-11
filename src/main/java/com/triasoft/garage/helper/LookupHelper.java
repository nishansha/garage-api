package com.triasoft.garage.helper;

import com.triasoft.garage.constants.LookupTypeEnum;
import com.triasoft.garage.constants.StatusEnum;
import com.triasoft.garage.entity.LookupMaster;
import com.triasoft.garage.repository.LookupMasterRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LookupHelper {

    private final LookupMasterRepository lookupMasterRepository;

    public LookupMaster getStatus(LookupTypeEnum lookupTypeEnum, StatusEnum statusEnum) {
        return lookupMasterRepository.findByTypeAndCodeAndEnabledTrue(lookupTypeEnum.name(), statusEnum.name());
    }

    public LookupMaster get(Long id) {
        return lookupMasterRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Color not found"));
    }

    public LookupMaster get(String code) {
        return lookupMasterRepository.findByCodeAndEnabledTrue(code);
    }

    public LookupMaster get(String type, String code) {
        return lookupMasterRepository.findByTypeAndCodeAndEnabledTrue(type, code);
    }
}
