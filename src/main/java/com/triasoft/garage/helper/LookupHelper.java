package com.triasoft.garage.helper;

import com.triasoft.garage.constants.LookupTypeEnum;
import com.triasoft.garage.constants.StatusEnum;
import com.triasoft.garage.entity.LookupMaster;
import com.triasoft.garage.repository.LookupMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LookupHelper {

    private final LookupMasterRepository lookupMasterRepository;

    public LookupMaster getStatus(LookupTypeEnum lookupTypeEnum, StatusEnum statusEnum) {
        return lookupMasterRepository.findByTypeAndCodeAndEnabledTrue(lookupTypeEnum.name(), statusEnum.name());
    }
}
