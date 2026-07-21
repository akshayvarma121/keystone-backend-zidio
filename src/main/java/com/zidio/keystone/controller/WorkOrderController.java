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
    public ResponseEntity<PageResponse<WorkOrderResponse>> getWorkOrders(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) WorkOrderStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) UUID assignedTo,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID siteId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(workOrderService.getWorkOrders(
                title, status, priority, assignedTo, customerId, siteId, pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<PageResponse<WorkOrderResponse>> searchWorkOrders(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(workOrderService.searchWorkOrders(q, pageable));
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

    @PostMapping("/{id}/rating")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> rateWorkOrder(@PathVariable UUID id, @Valid @RequestBody com.zidio.keystone.dto.request.WorkOrderRatingRequest request) {
        try {
            workOrderService.rateWorkOrder(id, request);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            if (e.getMessage().contains("already been rated")) {
                return ResponseEntity.status(409).build();
            }
            throw e;
        }
    }

    @PostMapping(value = "/{id}/attachments", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<com.zidio.keystone.dto.response.AttachmentResponse> uploadAttachment(
            @PathVariable UUID id,
            @org.springframework.web.bind.annotation.RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        return ResponseEntity.ok(workOrderService.uploadAttachment(id, file));
    }

    @GetMapping("/{id}/attachments")
    public ResponseEntity<java.util.List<com.zidio.keystone.dto.response.AttachmentResponse>> getAttachments(@PathVariable UUID id) {
        return ResponseEntity.ok(workOrderService.getAttachments(id));
    }

    @GetMapping("/{id}/attachments/{attId}")
    public ResponseEntity<org.springframework.core.io.Resource> downloadAttachment(
            @PathVariable UUID id,
            @PathVariable UUID attId) {
        com.zidio.keystone.domain.Attachment metadata = workOrderService.getAttachmentMetadata(id, attId);
        org.springframework.core.io.InputStreamResource resource = workOrderService.downloadAttachment(id, attId);
        
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(metadata.getContentType()))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + metadata.getFileName() + "\"")
                .body(resource);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<com.zidio.keystone.dto.response.CommentResponse> createComment(
            @PathVariable UUID id,
            @Valid @RequestBody com.zidio.keystone.dto.request.CommentRequest request) {
        return ResponseEntity.ok(workOrderService.createComment(id, request));
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<com.zidio.keystone.dto.response.PageResponse<com.zidio.keystone.dto.response.CommentResponse>> getComments(
            @PathVariable UUID id,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return ResponseEntity.ok(workOrderService.getComments(id, pageable));
    }
}
