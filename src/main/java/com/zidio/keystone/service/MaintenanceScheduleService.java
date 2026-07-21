package com.zidio.keystone.service;

import com.zidio.keystone.domain.*;
import com.zidio.keystone.dto.request.MaintenanceScheduleRequest;
import com.zidio.keystone.dto.response.MaintenanceScheduleResponse;
import com.zidio.keystone.repository.CustomerRepository;
import com.zidio.keystone.repository.MaintenanceScheduleRepository;
import com.zidio.keystone.repository.SiteRepository;
import com.zidio.keystone.repository.SkillRepository;
import com.zidio.keystone.repository.UserRepository;
import com.zidio.keystone.security.KeystoneUserDetails;
import com.zidio.keystone.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.zidio.keystone.dto.response.PageResponse;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaintenanceScheduleService {
    private final MaintenanceScheduleRepository maintenanceScheduleRepository;
    private final CustomerRepository customerRepository;
    private final SiteRepository siteRepository;
    private final SkillRepository skillRepository;
    private final UserRepository userRepository;

    @Transactional
    @PreAuthorize("hasRole('MANAGER')")
    public MaintenanceScheduleResponse createSchedule(MaintenanceScheduleRequest request) {
        KeystoneUserDetails userDetails = SecurityUtils.getCurrentUser();
        User currentUser = userRepository.getReferenceById(userDetails.getId());
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));
        Site site = siteRepository.findById(request.getSiteId())
                .orElseThrow(() -> new EntityNotFoundException("Site not found"));
                
        Skill skill = null;
        if (request.getRequiredSkillId() != null) {
            skill = skillRepository.findById(request.getRequiredSkillId())
                    .orElseThrow(() -> new EntityNotFoundException("Skill not found"));
        }

        MaintenanceSchedule schedule = MaintenanceSchedule.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority())
                .customer(customer)
                .site(site)
                .requiredSkill(skill)
                .frequencyDays(request.getFrequencyDays())
                .nextRunAt(request.getNextRunAt())
                .active(true)
                .createdBy(currentUser)
                .build();
                
        schedule = maintenanceScheduleRepository.save(schedule);
        return mapToResponse(schedule);
    }
    
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('MANAGER')")
    public PageResponse<MaintenanceScheduleResponse> getSchedules(Pageable pageable) {
        Page<MaintenanceSchedule> page = maintenanceScheduleRepository.findAll(pageable);
        return new PageResponse<>(
                page.getContent().stream().map(this::mapToResponse).collect(Collectors.toList()),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
    
    private MaintenanceScheduleResponse mapToResponse(MaintenanceSchedule s) {
        return MaintenanceScheduleResponse.builder()
                .id(s.getId())
                .title(s.getTitle())
                .description(s.getDescription())
                .priority(s.getPriority())
                .customerId(s.getCustomer().getId())
                .customerName(s.getCustomer().getName())
                .siteId(s.getSite().getId())
                .siteName(s.getSite().getName())
                .requiredSkillId(s.getRequiredSkill() != null ? s.getRequiredSkill().getId() : null)
                .requiredSkillName(s.getRequiredSkill() != null ? s.getRequiredSkill().getName() : null)
                .frequencyDays(s.getFrequencyDays())
                .nextRunAt(s.getNextRunAt())
                .active(s.getActive())
                .createdById(s.getCreatedBy().getId())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
