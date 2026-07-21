package com.zidio.keystone.mapper;

import com.zidio.keystone.domain.Customer;
import com.zidio.keystone.dto.request.CustomerRequest;
import com.zidio.keystone.dto.response.CustomerResponse;

public class CustomerMapper {

    public static CustomerResponse toResponse(Customer customer) {
        if (customer == null) return null;
        return CustomerResponse.builder()
                .id(customer.getId())
                .name(customer.getName())
                .contactEmail(customer.getContactEmail())
                .createdAt(customer.getCreatedAt())
                .build();
    }
    
    public static void updateEntity(Customer customer, CustomerRequest request) {
        customer.setName(request.getName());
        customer.setContactEmail(request.getContactEmail());
    }
}
