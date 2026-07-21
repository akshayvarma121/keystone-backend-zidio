package com.zidio.keystone.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "time_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id", nullable = false)
    private User technician;

    @Column(nullable = false)
    private Integer minutes;

    @Column(length = 500)
    private String note;

    @CreationTimestamp
    @Column(name = "logged_at", nullable = false, updatable = false)
    private OffsetDateTime loggedAt;
}
