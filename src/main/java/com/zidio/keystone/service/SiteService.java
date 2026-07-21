package com.zidio.keystone.service;

import com.zidio.keystone.domain.Customer;
import com.zidio.keystone.domain.Role;
import com.zidio.keystone.domain.Site;
import com.zidio.keystone.dto.request.SiteRequest;
import com.zidio.keystone.dto.response.PageResponse;
import com.zidio.keystone.dto.response.SiteResponse;
import com.zidio.keystone.mapper.SiteMapper;
import com.zidio.keystone.repository.CustomerRepository;
import com.zidio.keystone.repository.SiteRepository;
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
public class SiteService {

    private final SiteRepository siteRepository;
    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public PageResponse<SiteResponse> searchSites(String name, Pageable pageable) {
        KeystoneUserDetails user = SecurityUtils.getCurrentUser();
        UUID scopeCustomerId = user.getRole() == Role.CUSTOMER ? user.getCustomerId() : null;

        Page<Site> page = siteRepository.searchSites(name, scopeCustomerId, pageable);
        return PageResponse.of(page.map(SiteMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public SiteResponse getSite(UUID id) {
        Site site = siteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Site not found: " + id));

        KeystoneUserDetails user = SecurityUtils.getCurrentUser();
        if (user.getRole() == Role.CUSTOMER && !site.getCustomer().getId().equals(user.getCustomerId())) {
            throw new AccessDeniedException("Cannot access other customer's site");
        }

        return SiteMapper.toResponse(site);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('DISPATCHER', 'MANAGER')")
    public SiteResponse createSite(SiteRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + request.getCustomerId()));
        
        Site site = new Site();
        SiteMapper.updateEntity(site, request);
        site.setCustomer(customer);
        site = siteRepository.save(site);
        return SiteMapper.toResponse(site);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('DISPATCHER', 'MANAGER')")
    public SiteResponse updateSite(UUID id, SiteRequest request) {
        Site site = siteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Site not found: " + id));

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + request.getCustomerId()));

        SiteMapper.updateEntity(site, request);
        site.setCustomer(customer);
        return SiteMapper.toResponse(site);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('DISPATCHER', 'MANAGER')")
    public void deleteSite(UUID id) {
        if (!siteRepository.existsById(id)) {
            throw new EntityNotFoundException("Site not found: " + id);
        }
        siteRepository.deleteById(id);
    }
}
