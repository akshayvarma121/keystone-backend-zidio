package com.zidio.keystone.mapper;

import com.zidio.keystone.domain.Site;
import com.zidio.keystone.dto.request.SiteRequest;
import com.zidio.keystone.dto.response.SiteResponse;

public class SiteMapper {

    public static SiteResponse toResponse(Site site) {
        if (site == null) return null;
        return SiteResponse.builder()
                .id(site.getId())
                .customerId(site.getCustomer() != null ? site.getCustomer().getId() : null)
                .customerName(site.getCustomer() != null ? site.getCustomer().getName() : null)
                .name(site.getName())
                .address(site.getAddress())
                .createdAt(site.getCreatedAt())
                .build();
    }

    public static void updateEntity(Site site, SiteRequest request) {
        site.setName(request.getName());
        site.setAddress(request.getAddress());
    }
}
