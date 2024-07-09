package com.immoc.bilibili.dao;


import com.immoc.bilibili.domain.File;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

@Mapper
@Component
public interface FileDao {

    File getFileByMD5(String fileMd5);

    Integer addFile(File file);
}
