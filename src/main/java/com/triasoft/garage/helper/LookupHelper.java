package com.triasoft.garage.helper;

import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.constants.LookupTypeEnum;
import com.triasoft.garage.constants.StatusEnum;
import com.triasoft.garage.entity.LookupMaster;
import com.triasoft.garage.exception.BusinessException;
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

    public LookupMaster get(Long id) {
        return lookupMasterRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.Business.LOOKUP_NOT_EXISTS));
    }

    public LookupMaster get(String code) {
        return lookupMasterRepository.findByCodeAndEnabledTrue(code);
    }

    public LookupMaster get(String type, String code) {
        return lookupMasterRepository.findByTypeAndCodeAndEnabledTrue(type, code);
    }
}
