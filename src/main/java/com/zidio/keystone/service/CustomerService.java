package com.zidio.keystone.service;

import com.zidio.keystone.domain.Customer;
import com.zidio.keystone.domain.Role;
import com.zidio.keystone.dto.request.CustomerRequest;
import com.zidio.keystone.dto.response.CustomerResponse;
import com.zidio.keystone.dto.response.PageResponse;
import com.zidio.keystone.mapper.CustomerMapper;
import com.zidio.keystone.repository.CustomerRepository;
import com.zidio.keystone.security.KeystoneUserDetails;
import com.zidio.keystone.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> searchCustomers(String name, Pageable pageable) {
        KeystoneUserDetails user = SecurityUtils.getCurrentUser();
        UUID scopeCustomerId = user.getRole() == Role.CUSTOMER ? user.getCustomerId() : null;

        Page<Customer> page = customerRepository.searchCustomers(name, scopeCustomerId, pageable);
        return PageResponse.of(page.map(CustomerMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(UUID id) {
        KeystoneUserDetails user = SecurityUtils.getCurrentUser();
        if (user.getRole() == Role.CUSTOMER && !id.equals(user.getCustomerId())) {
            throw new AccessDeniedException("Cannot access other customer data");
        }

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + id));
        return CustomerMapper.toResponse(customer);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('DISPATCHER', 'MANAGER')")
    public CustomerResponse createCustomer(CustomerRequest request) {
        Customer customer = new Customer();
        CustomerMapper.updateEntity(customer, request);
        customer = customerRepository.save(customer);
        return CustomerMapper.toResponse(customer);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('DISPATCHER', 'MANAGER')")
    public CustomerResponse updateCustomer(UUID id, CustomerRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + id));
        CustomerMapper.updateEntity(customer, request);
        return CustomerMapper.toResponse(customer);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('DISPATCHER', 'MANAGER')")
    public void deleteCustomer(UUID id) {
        if (!customerRepository.existsById(id)) {
            throw new EntityNotFoundException("Customer not found: " + id);
        }
        customerRepository.deleteById(id);
    }
}
