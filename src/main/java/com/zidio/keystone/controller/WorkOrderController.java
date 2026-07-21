package com.zidio.keystone.controller;

import com.zidio.keystone.domain.Priority;
import com.zidio.keystone.domain.WorkOrderStatus;
import com.zidio.keystone.dto.request.WorkOrderRequest;
import com.zidio.keystone.dto.response.PageResponse;
import com.zidio.keystone.dto.response.WorkOrderDetailsResponse;
import com.zidio.keystone.dto.response.WorkOrderResponse;
import com.zidio.keystone.service.WorkOrderService;
import com.zidio.keystone.service.statemachine.WorkOrderLifecycle;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/work-orders")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService workOrderService;
    private final WorkOrderLifecycle workOrderLifecycle;

    @PostMapping
    public ResponseEntity<WorkOrderResponse> createWorkOrder(@Valid @RequestBody WorkOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workOrderService.createWorkOrder(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkOrderDetailsResponse> getWorkOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(workOrderService.getWorkOrder(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<WorkOrderResponse>> searchWorkOrders(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) WorkOrderStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) UUID assignedTo,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID siteId,
            Pageable pageable) {
        return ResponseEntity.ok(workOrderService.searchWorkOrders(title, status, priority, assignedTo, customerId, siteId, pageable));
    }

    @GetMapping("/{id}/assignment-candidates")
    public ResponseEntity<List<com.zidio.keystone.dto.response.AssignmentCandidateResponse>> getAssignmentCandidates(@PathVariable UUID id) {
        return ResponseEntity.ok(workOrderService.getAssignmentCandidates(id));
    }

    @GetMapping("/board")
    public ResponseEntity<Map<WorkOrderStatus, List<WorkOrderResponse>>> getBoard() {
        return ResponseEntity.ok(workOrderService.getBoard());
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkOrderResponse> updateWorkOrder(
            @PathVariable UUID id,
            @Valid @RequestBody WorkOrderRequest request) {
        return ResponseEntity.ok(workOrderService.updateWorkOrder(id, request));
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<WorkOrderResponse> assignWorkOrder(
            @PathVariable UUID id,
            @Valid @RequestBody com.zidio.keystone.dto.request.WorkOrderAssignRequest request) {
        return ResponseEntity.ok(workOrderLifecycle.assign(id, request.getAssigneeId(), request.getNote()));
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<WorkOrderResponse> updateWorkOrderStatus(
            @PathVariable UUID id,
            @Valid @RequestBody com.zidio.keystone.dto.request.WorkOrderTransitionRequest request) {
        return ResponseEntity.ok(workOrderLifecycle.transition(id, request.getTargetStatus(), request.getNote()));
    }

    @PostMapping("/{id}/parts")
    public ResponseEntity<com.zidio.keystone.dto.response.PartUsageResponse> logPartUsage(
            @PathVariable UUID id,
            @Valid @RequestBody com.zidio.keystone.dto.request.PartUsageRequest request) {
        return ResponseEntity.ok(workOrderService.logPartUsage(id, request));
    }

    @PostMapping("/{id}/time")
    public ResponseEntity<com.zidio.keystone.dto.response.TimeLogResponse> logTime(
            @PathVariable UUID id,
            @Valid @RequestBody com.zidio.keystone.dto.request.TimeLogRequest request) {
        return ResponseEntity.ok(workOrderService.logTime(id, request));
    }
}
