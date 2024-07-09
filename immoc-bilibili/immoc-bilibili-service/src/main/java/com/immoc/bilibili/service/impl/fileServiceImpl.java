package com.immoc.bilibili.service.impl;

import com.immoc.bilibili.dao.FileDao;
import com.immoc.bilibili.domain.File;
import com.immoc.bilibili.service.fileService;
import com.immoc.bilibili.service.util.FastDfsUtil;
import com.immoc.bilibili.service.util.MD5Util;
import io.netty.util.internal.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;


@Service
public class fileServiceImpl implements fileService {

    @Autowired
    private FileDao fileDao;

    @Autowired
    private FastDfsUtil fastDfsUtil;

    @Override
    public String getFileMd5(MultipartFile file) throws Exception {
        return MD5Util.getFileMD5(file);
    }

    @Override
    public String uploadFileBySlices(MultipartFile slice, String fileMd5, Integer sliceNo, Integer totalSliceNo) throws IOException {
        File dbFileMD5 = fileDao.getFileByMD5(fileMd5);
        //秒传功能
        if(dbFileMD5 != null){
            return dbFileMD5.getUrl();
        }
        String url = fastDfsUtil.uploadFileBySlices(slice, fileMd5, sliceNo, totalSliceNo);
        if(!StringUtil.isNullOrEmpty(url)){
            dbFileMD5 = new File();
            dbFileMD5.setCreateTime(new Date());
            dbFileMD5.setMd5(fileMd5);
            dbFileMD5.setUrl(url);
            dbFileMD5.setType(fastDfsUtil.getFileType(slice));
            fileDao.addFile(dbFileMD5);
        }
        return url;
    }
}
