package com.zidio.keystone.controller;

import com.zidio.keystone.dto.request.MaintenanceScheduleRequest;
import com.zidio.keystone.dto.response.MaintenanceScheduleResponse;
import com.zidio.keystone.dto.response.PageResponse;
import com.zidio.keystone.service.MaintenanceScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/maintenance-schedules")
@RequiredArgsConstructor
public class MaintenanceScheduleController {

    private final MaintenanceScheduleService maintenanceScheduleService;

    @PostMapping
    public ResponseEntity<MaintenanceScheduleResponse> createSchedule(
            @Valid @RequestBody MaintenanceScheduleRequest request) {
        return ResponseEntity.ok(maintenanceScheduleService.createSchedule(request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<MaintenanceScheduleResponse>> getSchedules(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(maintenanceScheduleService.getSchedules(pageable));
    }
}
