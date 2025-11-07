package com.triasoft.garage.service;

import com.triasoft.garage.dto.LookupDTO;
import com.triasoft.garage.entity.LookupMaster;
import com.triasoft.garage.model.common.LookupRq;
import com.triasoft.garage.model.common.LookupRs;
import com.triasoft.garage.repository.LookupMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LookupService {

    private final LookupMasterRepository lookupMasterRepository;

    public LookupRs getLookupValues(LookupRq lookupRq) {
        List<LookupMaster> lookupMasters =  lookupMasterRepository.findByTypeAndEnabledTrue(lookupRq.getType());
        if(CollectionUtils.isEmpty(lookupMasters))
            return LookupRs.builder().values(List.of()).build();
        return LookupRs.builder().values(lookupMasters.stream().map(this::toLookupDTO).toList()).build();
    }

    private LookupDTO toLookupDTO(LookupMaster lookupMaster) {
        LookupDTO lookupDTO = new LookupDTO();
        lookupDTO.setId(lookupMaster.getId());
        lookupDTO.setCode(lookupMaster.getCode());
        lookupDTO.setDescription(lookupMaster.getDescription());
        return lookupDTO;
    }
}
