package com.triasoft.garage.service.impl;

import com.triasoft.garage.dto.CustomerSummaryDTO;
import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.model.customer.CustomerRs;
import com.triasoft.garage.projection.CustomerBalanceRow;
import com.triasoft.garage.repository.CustomerRepository;
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
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerRs getCustomers(Pageable pageable, UserDTO user) {
        Page<CustomerBalanceRow> customerPage = customerRepository.findCustomersWithOutstandingBalance(pageable);
        List<CustomerSummaryDTO> customers = customerPage.getContent().stream()
                .map(this::convertToDTO)
                .toList();
        CustomerRs customerRs = CustomerRs.builder().customers(customers).build();
        customerRs.setTotalPages(customerPage.getTotalPages());
        customerRs.setTotalElements(customerPage.getTotalElements());
        return customerRs;
    }

    private CustomerSummaryDTO convertToDTO(CustomerBalanceRow row) {
        return CustomerSummaryDTO.builder()
                .id(row.getId())
                .name(row.getName())
                .mobile(row.getMobile())
                .address(row.getAddress())
                .outstandingBalance(row.getOutstandingBalance() != null ? row.getOutstandingBalance() : BigDecimal.ZERO)
                .build();
    }
}
