package com.triasoft.garage.model.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttachmentRs implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String fileName;
    private String contentType;
    private String category;
    private Long fileSize;
    private LocalDateTime createdAt;
    private String downloadUrl;
}
