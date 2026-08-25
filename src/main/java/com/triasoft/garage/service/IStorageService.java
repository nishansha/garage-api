package com.triasoft.garage.service;

import com.triasoft.garage.dto.UserDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

public interface IStorageService {
    List<String> upload(String objectPrefix, List<MultipartFile> files, UserDTO user);

    InputStream download(String objectKey);
}
