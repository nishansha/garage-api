package com.triasoft.garage.controller;

import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.model.common.ApiResponse;
import com.triasoft.garage.model.common.AttachmentRs;
import com.triasoft.garage.model.common.UploadRs;
import com.triasoft.garage.service.impl.UploadService;
import com.triasoft.garage.util.UserUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/upload")
public class UploadController {

    private final UploadService uploadService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<UploadRs>> upload(
            @RequestParam("entityType") String entityType,
            @RequestParam("entityId") Long entityId,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam("files") List<MultipartFile> files,
            HttpServletRequest request) {
        log.info(":: UploadController - upload() - entityType={}, entityId={}, category={}, fileCount={} ::", entityType, entityId, category, files.size());
        UserDTO user = UserUtil.getUser(request);
        return ResponseEntity.ok(ApiResponse.success(uploadService.upload(entityType, entityId, category, files, user)));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<AttachmentRs>>> list(
            @RequestParam("entityType") String entityType,
            @RequestParam("entityId") Long entityId,
            @RequestParam(value = "category", required = false) String category) {
        return ResponseEntity.ok(ApiResponse.success(uploadService.list(entityType, entityId, category)));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable("id") Long id) {
        UploadService.DownloadedFile file = uploadService.download(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.fileName() + "\"")
                .body(new InputStreamResource(file.stream()));
    }
}
