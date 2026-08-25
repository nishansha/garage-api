package com.triasoft.garage.service;

import com.triasoft.garage.config.AppProperties;
import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.exception.BusinessException;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Profile("minio")
public class MinioStorageServiceImpl implements IStorageService {

    private final MinioClient minioClient;
    private final AppProperties properties;

    public MinioStorageServiceImpl(MinioClient minioClient, AppProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    @Override
    public List<String> upload(String objectPrefix, List<MultipartFile> files, UserDTO user) {
        List<String> objectKeys = new ArrayList<>();
        files.forEach(file -> {
            String objectKey = objectPrefix + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
            try {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(properties.getStorage().getMinio().getBucket())
                                .object(objectKey)
                                .stream(file.getInputStream(), file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build()
                );
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.General.UPLOAD_FAILED);
            }
            objectKeys.add(objectKey);
        });
        return objectKeys;
    }

    @Override
    public InputStream download(String objectKey) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(properties.getStorage().getMinio().getBucket())
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.General.DOWNLOAD_FAILED);
        }
    }
}
