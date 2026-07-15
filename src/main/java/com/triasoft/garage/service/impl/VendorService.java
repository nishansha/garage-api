package com.triasoft.garage.service.impl;

import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.dto.VendorSummaryDTO;
import com.triasoft.garage.model.vendor.VendorRs;
import com.triasoft.garage.projection.VendorBalanceRow;
import com.triasoft.garage.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VendorService {

    private final VendorRepository vendorRepository;

    public VendorRs getVendors(Pageable pageable, UserDTO user) {
        Page<VendorBalanceRow> vendorPage = vendorRepository.findVendorsWithOutstandingBalance(pageable);
        List<VendorSummaryDTO> vendors = vendorPage.getContent().stream()
                .map(this::convertToDTO)
                .toList();
        VendorRs vendorRs = VendorRs.builder().vendors(vendors).build();
        vendorRs.setTotalPages(vendorPage.getTotalPages());
        vendorRs.setTotalElements(vendorPage.getTotalElements());
        return vendorRs;
    }

    private VendorSummaryDTO convertToDTO(VendorBalanceRow row) {
        return VendorSummaryDTO.builder()
                .id(row.getId())
                .name(row.getName())
                .mobile(row.getMobile())
                .address(row.getAddress())
                .outstandingBalance(row.getOutstandingBalance() != null ? row.getOutstandingBalance() : BigDecimal.ZERO)
                .build();
    }
}
