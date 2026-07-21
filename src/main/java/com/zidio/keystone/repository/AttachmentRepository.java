package com.zidio.keystone.repository;

import com.zidio.keystone.domain.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {
    List<Attachment> findByWorkOrderIdOrderByUploadedAtDesc(UUID workOrderId);
}
