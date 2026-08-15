package com.triasoft.garage.service.impl;

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

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final InventoryRepository inventoryRepository;

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
        Warehouse warehouse = new Warehouse();
        warehouse.setCode(code);
        warehouse.setName(rq.getName());
        warehouse.setAddress(rq.getAddress());
        warehouse.setLocation(rq.getLocation());
        warehouseRepository.save(warehouse);
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
        return WarehouseRs.builder().id(warehouse.getId()).build();
    }

    @Transactional
    public void delete(Long id) {
        Warehouse warehouse = findById(id);
        if (inventoryRepository.existsByWarehouseId(id)) {
            throw new BusinessException(ErrorCode.Business.WAREHOUSE_IN_USE);
        }
        warehouseRepository.delete(warehouse);
    }

    private Warehouse findById(Long id) {
        return warehouseRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.Business.WAREHOUSE_NOT_FOUND));
    }

    private WarehouseDTO toDTO(Warehouse warehouse) {
        return WarehouseDTO.builder()
                .id(warehouse.getId())
                .code(warehouse.getCode())
                .name(warehouse.getName())
                .address(warehouse.getAddress())
                .location(warehouse.getLocation())
                .build();
    }
}
