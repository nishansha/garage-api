package com.triasoft.garage.service.impl;

import com.triasoft.garage.company.entity.WarehouseBusinessLine;
import com.triasoft.garage.company.repository.CompanyRepository;
import com.triasoft.garage.company.repository.WarehouseBusinessLineRepository;
import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.dto.WarehouseDTO;
import com.triasoft.garage.entity.Warehouse;
import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.locking.VersionCheck;
import com.triasoft.garage.model.warehouse.WarehouseRq;
import com.triasoft.garage.model.warehouse.WarehouseRs;
import com.triasoft.garage.repository.InventoryRepository;
import com.triasoft.garage.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final InventoryRepository inventoryRepository;
    private final CompanyRepository companyRepository;
    private final WarehouseBusinessLineRepository warehouseBusinessLineRepository;

    public WarehouseRs getAll() {
        List<WarehouseDTO> warehouses = warehouseRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
        return WarehouseRs.builder().warehouses(warehouses).build();
    }

    public WarehouseDTO get(Long id) {
        return toDTO(findById(id));
    }

    @Transactional
    public WarehouseRs create(WarehouseRq rq) {
        String code = rq.getCode().trim().toUpperCase();
        if (warehouseRepository.existsByCodeIgnoreCase(code)) {
            throw new BusinessException(ErrorCode.Business.WAREHOUSE_CODE_EXISTS);
        }
        if (!companyRepository.existsById(rq.getCompanyId())) {
            throw new BusinessException(ErrorCode.Business.COMPANY_NOT_FOUND);
        }
        Warehouse warehouse = new Warehouse();
        warehouse.setCompanyId(rq.getCompanyId());
        warehouse.setCode(code);
        warehouse.setName(rq.getName());
        warehouse.setAddress(rq.getAddress());
        warehouse.setLocation(rq.getLocation());
        warehouseRepository.save(warehouse);
        saveBusinessLines(warehouse.getId(), rq.getBusinessLines());
        return WarehouseRs.builder().id(warehouse.getId()).build();
    }

    @Transactional
    @VersionCheck(entity = Warehouse.class)
    public WarehouseRs update(Long id, WarehouseRq rq) {
        Warehouse warehouse = findById(id);
        String code = rq.getCode().trim().toUpperCase();
        if (warehouseRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new BusinessException(ErrorCode.Business.WAREHOUSE_CODE_EXISTS);
        }
        warehouse.setCode(code);
        warehouse.setName(rq.getName());
        warehouse.setAddress(rq.getAddress());
        warehouse.setLocation(rq.getLocation());
        warehouseRepository.save(warehouse);
        warehouseBusinessLineRepository.deleteByWarehouseId(id);
        warehouseBusinessLineRepository.flush();
        saveBusinessLines(id, rq.getBusinessLines());
        return WarehouseRs.builder().id(warehouse.getId()).build();
    }

    @Transactional
    public void delete(Long id) {
        Warehouse warehouse = findById(id);
        if (inventoryRepository.existsByWarehouseId(id)) {
            throw new BusinessException(ErrorCode.Business.WAREHOUSE_IN_USE);
        }
        warehouseBusinessLineRepository.deleteByWarehouseId(id);
        warehouseRepository.delete(warehouse);
    }

    private void saveBusinessLines(Long warehouseId, Set<com.triasoft.garage.company.constants.BusinessLine> businessLines) {
        List<WarehouseBusinessLine> rows = businessLines.stream().map(bl -> {
            WarehouseBusinessLine row = new WarehouseBusinessLine();
            row.setWarehouseId(warehouseId);
            row.setBusinessLine(bl);
            return row;
        }).toList();
        warehouseBusinessLineRepository.saveAll(rows);
    }

    private Warehouse findById(Long id) {
        return warehouseRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.Business.WAREHOUSE_NOT_FOUND));
    }

    private WarehouseDTO toDTO(Warehouse warehouse) {
        Set<com.triasoft.garage.company.constants.BusinessLine> businessLines = warehouseBusinessLineRepository.findByWarehouseId(warehouse.getId())
                .stream().map(WarehouseBusinessLine::getBusinessLine).collect(Collectors.toSet());
        return WarehouseDTO.builder()
                .id(warehouse.getId())
                .version(warehouse.getVersion())
                .companyId(warehouse.getCompanyId())
                .businessLines(businessLines)
                .code(warehouse.getCode())
                .name(warehouse.getName())
                .address(warehouse.getAddress())
                .location(warehouse.getLocation())
                .build();
    }
}
