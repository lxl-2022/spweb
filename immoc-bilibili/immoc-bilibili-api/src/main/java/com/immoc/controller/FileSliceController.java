package com.immoc.controller;


import com.immoc.bilibili.domain.JsonResponse;
import com.immoc.bilibili.service.fileService;
import com.immoc.bilibili.service.impl.fileServiceImpl;
import com.immoc.bilibili.service.util.FastDfsUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
public class FileSliceController {

    @Autowired
    private  FastDfsUtil fastDfsUtil;

     @Autowired
     private com.immoc.bilibili.service.fileService fileService;

    @PostMapping("/md5files")
     public JsonResponse<String> getFileMD5(@RequestParam MultipartFile file) throws Exception {
         String md5=fileService.getFileMd5(file);
         return new JsonResponse<>(md5);
     }

    @GetMapping("slice")
    public JsonResponse slice(@RequestParam MultipartFile file) throws IOException {
        fastDfsUtil.convertFileToSlice(file);
        return JsonResponse.success();
    }

    @PutMapping("/file-slices")
    public JsonResponse<String> uploadFileBySlices(MultipartFile slice,
                                                   String fileMd5,
                                                   Integer sliceNo,
                                                   Integer totalSliceNo) throws Exception {
        String filePath = fileService.uploadFileBySlices(slice, fileMd5, sliceNo, totalSliceNo);
        return new JsonResponse<>(filePath);
    }
}
