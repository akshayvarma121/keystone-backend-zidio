package com.zidio.keystone.controller;

import com.zidio.keystone.dto.request.SiteRequest;
import com.zidio.keystone.dto.response.PageResponse;
import com.zidio.keystone.dto.response.SiteResponse;
import com.zidio.keystone.service.SiteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/sites")
@RequiredArgsConstructor
public class SiteController {

    private final SiteService siteService;

    @GetMapping
    public ResponseEntity<PageResponse<SiteResponse>> searchSites(
            @RequestParam(required = false) String name,
            Pageable pageable) {
        return ResponseEntity.ok(siteService.searchSites(name, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SiteResponse> getSite(@PathVariable UUID id) {
        return ResponseEntity.ok(siteService.getSite(id));
    }

    @PostMapping
    public ResponseEntity<SiteResponse> createSite(@Valid @RequestBody SiteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(siteService.createSite(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SiteResponse> updateSite(
            @PathVariable UUID id,
            @Valid @RequestBody SiteRequest request) {
        return ResponseEntity.ok(siteService.updateSite(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSite(@PathVariable UUID id) {
        siteService.deleteSite(id);
        return ResponseEntity.noContent().build();
    }
}
