package com.triasoft.garage.repository;

import com.triasoft.garage.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByEntityTypeAndEntityId(String entityType, Long entityId);

    List<Attachment> findByEntityTypeAndEntityIdAndCategory(String entityType, Long entityId, String category);
}
