package com.triasoft.garage.servicesale.service;

import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.locking.VersionCheck;
import com.triasoft.garage.repository.WarehouseRepository;
import com.triasoft.garage.servicesale.dto.ServiceOfferingDTO;
import com.triasoft.garage.servicesale.entity.ServiceOffering;
import com.triasoft.garage.servicesale.model.ServiceOfferingRq;
import com.triasoft.garage.servicesale.model.ServiceOfferingRs;
import com.triasoft.garage.servicesale.repository.ServiceOfferingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceOfferingService {

    private final ServiceOfferingRepository serviceOfferingRepository;
    private final WarehouseRepository warehouseRepository;

    public ServiceOfferingRs getAll(Long warehouseId) {
        List<ServiceOffering> offerings = warehouseId != null
                ? serviceOfferingRepository.findByWarehouseId(warehouseId)
                : serviceOfferingRepository.findAll();
        return ServiceOfferingRs.builder().services(offerings.stream().map(this::toDTO).toList()).build();
    }

    public ServiceOfferingDTO get(Long id) {
        return toDTO(findById(id));
    }

    @Transactional
    public ServiceOfferingRs create(ServiceOfferingRq rq) {
        if (!warehouseRepository.existsById(rq.getWarehouseId())) {
            throw new BusinessException(ErrorCode.Business.WAREHOUSE_NOT_FOUND);
        }
        String code = rq.getCode().trim().toUpperCase();
        if (serviceOfferingRepository.existsByWarehouseIdAndCodeIgnoreCase(rq.getWarehouseId(), code)) {
            throw new BusinessException(ErrorCode.Business.SERVICE_OFFERING_CODE_EXISTS);
        }
        ServiceOffering offering = new ServiceOffering();
        offering.setWarehouseId(rq.getWarehouseId());
        offering.setCode(code);
        offering.setName(rq.getName());
        offering.setDefaultRate(rq.getDefaultRate());
        offering.setActive(rq.isActive());
        serviceOfferingRepository.save(offering);
        return ServiceOfferingRs.builder().id(offering.getId()).build();
    }

    @Transactional
    @VersionCheck(entity = ServiceOffering.class)
    public ServiceOfferingRs update(Long id, ServiceOfferingRq rq) {
        ServiceOffering offering = findById(id);
        String code = rq.getCode().trim().toUpperCase();
        if (serviceOfferingRepository.existsByWarehouseIdAndCodeIgnoreCaseAndIdNot(offering.getWarehouseId(), code, id)) {
            throw new BusinessException(ErrorCode.Business.SERVICE_OFFERING_CODE_EXISTS);
        }
        offering.setCode(code);
        offering.setName(rq.getName());
        offering.setDefaultRate(rq.getDefaultRate());
        offering.setActive(rq.isActive());
        serviceOfferingRepository.save(offering);
        return ServiceOfferingRs.builder().id(offering.getId()).build();
    }

    @Transactional
    public void delete(Long id) {
        serviceOfferingRepository.delete(findById(id));
    }

    private ServiceOffering findById(Long id) {
        return serviceOfferingRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.SERVICE_OFFERING_NOT_FOUND));
    }

    private ServiceOfferingDTO toDTO(ServiceOffering offering) {
        return ServiceOfferingDTO.builder()
                .id(offering.getId())
                .version(offering.getVersion())
                .warehouseId(offering.getWarehouseId())
                .code(offering.getCode())
                .name(offering.getName())
                .defaultRate(offering.getDefaultRate())
                .active(offering.isActive())
                .build();
    }
}
