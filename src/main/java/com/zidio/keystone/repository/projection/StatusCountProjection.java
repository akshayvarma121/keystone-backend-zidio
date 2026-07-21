package com.zidio.keystone.repository.projection;

import com.zidio.keystone.domain.WorkOrderStatus;

public interface StatusCountProjection {
    WorkOrderStatus getStatus();
    Long getCount();
}
