package com.immoc.bilibili.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface fileService {
    String getFileMd5(MultipartFile file) throws Exception;

    String uploadFileBySlices(MultipartFile slice, String fileMd5, Integer sliceNo, Integer totalSliceNo) throws IOException;

}
