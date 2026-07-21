package com.zidio.keystone.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "part_usage")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_id", nullable = false)
    private Part part;

    @Column(name = "qty_used", nullable = false)
    private Integer qtyUsed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logged_by", nullable = false)
    private User loggedBy;

    @CreationTimestamp
    @Column(name = "logged_at", nullable = false, updatable = false)
    private OffsetDateTime loggedAt;
}
