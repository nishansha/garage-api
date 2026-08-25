package com.triasoft.garage.service.impl;

import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.entity.Attachment;
import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.model.common.AttachmentRs;
import com.triasoft.garage.model.common.UploadRs;
import com.triasoft.garage.repository.AttachmentRepository;
import com.triasoft.garage.service.IStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class UploadService {

    private final IStorageService storageService;
    private final AttachmentRepository attachmentRepository;

    public UploadRs upload(String entityType, Long entityId, String category, List<MultipartFile> files, UserDTO user) {
        String objectPrefix = entityType + "/" + entityId;
        List<String> objectKeys = storageService.upload(objectPrefix, files, user);
        List<AttachmentRs> attachments = IntStream.range(0, files.size())
                .mapToObj(i -> attachmentRepository.save(buildAttachment(entityType, entityId, category, objectKeys.get(i), files.get(i))))
                .map(this::toRs)
                .toList();
        return UploadRs.builder().attachments(attachments).build();
    }

    public List<AttachmentRs> list(String entityType, Long entityId, String category) {
        List<Attachment> attachments = category == null
                ? attachmentRepository.findByEntityTypeAndEntityId(entityType, entityId)
                : attachmentRepository.findByEntityTypeAndEntityIdAndCategory(entityType, entityId, category);
        return attachments.stream().map(this::toRs).toList();
    }

    public DownloadedFile download(Long id) {
        Attachment attachment = attachmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.ATTACHMENT_NOT_FOUND));
        InputStream stream = storageService.download(attachment.getObjectKey());
        return new DownloadedFile(stream, attachment.getContentType(), attachment.getFileName());
    }

    private Attachment buildAttachment(String entityType, Long entityId, String category, String objectKey, MultipartFile file) {
        Attachment attachment = new Attachment();
        attachment.setEntityType(entityType);
        attachment.setEntityId(entityId);
        attachment.setCategory(category);
        attachment.setObjectKey(objectKey);
        attachment.setFileName(file.getOriginalFilename());
        attachment.setContentType(file.getContentType());
        attachment.setFileSize(file.getSize());
        return attachment;
    }

    private AttachmentRs toRs(Attachment attachment) {
        return AttachmentRs.builder()
                .id(attachment.getId())
                .fileName(attachment.getFileName())
                .contentType(attachment.getContentType())
                .category(attachment.getCategory())
                .fileSize(attachment.getFileSize())
                .createdAt(attachment.getCreatedAt())
                .downloadUrl("/api/v1/upload/" + attachment.getId() + "/download")
                .build();
    }

    public record DownloadedFile(InputStream stream, String contentType, String fileName) {
    }
}
