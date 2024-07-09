package com.immoc.bilibili.service.util;

import com.github.tobato.fastdfs.domain.fdfs.FileInfo;
import com.github.tobato.fastdfs.domain.fdfs.MetaData;
import com.github.tobato.fastdfs.domain.fdfs.StorePath;
import com.github.tobato.fastdfs.service.AppendFileStorageClient;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.immoc.bilibili.domain.exception.ConditionException;
import io.netty.util.internal.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;


@Component
public class FastDfsUtil {

    @Autowired
    private FastFileStorageClient fastFileStorageClient;

    @Autowired
    private AppendFileStorageClient appendFileStorageClient;


    @Autowired
    private RedisTemplate<String,String> redisTemplate;



    private static  final String DEFAULT_GROUP="group1";
    private static final String PATH_KEY="path-key:";
    private static final String UPLOADED_KEY="uploaded-size-key:";
    private static final String UPLOADED_num_KEY="uploaded-num-key:";
    private static final int SLICE_SIEZ=1024*1024*10; //5M

    //获取文件类型
    public String getFileType(MultipartFile file){
        if(file==null){
            throw  new ConditionException("非法文件!");
        }
        //获取文件名
        String filename = file.getOriginalFilename();
        int index = filename.lastIndexOf(".");
        String fileType=filename.substring(index+1,filename.length());
        return fileType;
    }

    //上传文件的接口
    public String uploadCoomenFile(MultipartFile file) throws IOException {
        Set<MetaData> metaDataSet = new HashSet<>();
        String fileType = this.getFileType(file);
        //存储上传后的所有文件信息
        StorePath storePath = fastFileStorageClient.uploadFile(file.getInputStream(), file.getSize(), fileType, metaDataSet);
        return storePath.getPath();
    }

    //断点续传上传(上传第一个分片文件 返回上传分片的路径)
    public String uploadAppenderFile(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        String fileType = this.getFileType(file);
        StorePath storePath = appendFileStorageClient.uploadAppenderFile(DEFAULT_GROUP, file.getInputStream(), file.getSize(), fileType);
        return storePath.getPath();
    }

    //根据分片的路径 继续上传后续的文件
    public void  modiftAppenderFile(MultipartFile file,String filepath,long offset) throws IOException {
        appendFileStorageClient.modifyFile(DEFAULT_GROUP,filepath,file.getInputStream(),file.getSize(),offset);
    }

    //md5加密 与其他文件区分 可以实现秒传功能（如果上传过就会秒传 返回客户端上传完成）
    public String uploadFileBySlices(MultipartFile file,String fileMD5,Integer sliceNo,Integer sliceTotal) throws IOException {
        if(file==null || sliceNo==null || sliceTotal==null){
            throw new ConditionException("参数异常");
        }
        //上传的路径
        String pathkey=PATH_KEY+fileMD5;
        //上传文件大小
        String uploadedSizeKey=UPLOADED_KEY+fileMD5;
        //上传分片个数
        String uploadedNokey=UPLOADED_num_KEY+fileMD5;
        //首先判断当前上传的文件大小是否为0 判断是否上传的是第一个分片
        String uploadedSizeStr = redisTemplate.opsForValue().get(uploadedSizeKey);
        Long filesize=0L;
        if(!StringUtil.isNullOrEmpty(uploadedSizeStr)){
            filesize=Long.valueOf(uploadedSizeStr);
        }
        String fileType = this.getFileType(file);
        if(sliceNo==1){  //上传的第一个分片
            String stordePath = this.uploadAppenderFile(file);
            if(StringUtil.isNullOrEmpty(stordePath)){
                throw new ConditionException("上传失败");
            }
            //将上传的路径传递给redis
            redisTemplate.opsForValue().set(pathkey,stordePath);
            redisTemplate.opsForValue().set(uploadedNokey,"1");
        }else {
            String storePath = redisTemplate.opsForValue().get(pathkey);
            if(StringUtil.isNullOrEmpty(storePath)){
                throw  new ConditionException("上传失败");
            }
            //分片上传大小
            this.modiftAppenderFile(file,storePath,filesize);
            redisTemplate.opsForValue().increment(uploadedNokey);
        }
        //更新上传的大小
        filesize+=file.getSize();
        redisTemplate.opsForValue().set(uploadedSizeKey,String.valueOf(filesize));
        //如果所有分片都上传成功 则清空redis的所有key 只保存上传的文件地址
        String uploadnumstr = redisTemplate.opsForValue().get(uploadedNokey);
        Integer uploadslip = Integer.valueOf(uploadnumstr);
        //最终上传的路径
        String StorePath="";
        if(uploadslip.equals(sliceTotal)){
            StorePath=redisTemplate.opsForValue().get(pathkey);
            List<String> keylist= Arrays.asList(pathkey,uploadedSizeKey,uploadedNokey);
            redisTemplate.delete(keylist);
        }
        return StorePath;
    }

    //文件分片
    public void convertFileToSlice(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        String fileType = this.getFileType(file);
        //转换成File类型了
        File  Javafile=this.mutilpartFileTofile(file);
        long fileLength=Javafile.length();
        int count=1;
        for(int i=0;i<fileLength;i+=SLICE_SIEZ){
            RandomAccessFile randomAccessFile=new RandomAccessFile(Javafile,"r");
            randomAccessFile.seek(i);
            byte [] bytes=new byte[SLICE_SIEZ];
            int len=randomAccessFile.read(bytes); //再读最后一片不一定是规定的分片大小
            String tempproFile="D:\\temporFile\\"+count+"."+fileType; //临时存储分片位置
            File slice=new File(tempproFile);
            FileOutputStream fos = new FileOutputStream(slice);
            fos.write(bytes,0,len);
            fos.close();
            randomAccessFile.close();
            count++;
        }
        Javafile.delete();
    }

    //转换成File类型数据
    public File mutilpartFileTofile(MultipartFile multipartFile) throws IOException {
        String filename = multipartFile.getOriginalFilename();
        String[] filenameArr = filename.split("\\.");
        //参数一：文件名称  参数二：文件类型
        File file=File.createTempFile(filenameArr[0],"."+filenameArr[1]);
        multipartFile.transferTo(file);
        return file;
    }



    //删除
    public void deleteFile(String filePath){
        fastFileStorageClient.deleteFile(filePath);
    }

    @Value("${fast.storege-url}")
    private String httpFdfsStorageAddr;

    public void viewVideoOnlineBySlices(HttpServletRequest request, HttpServletResponse response, String path) throws Exception {
        //获取文件的信息
        FileInfo fileInfo = fastFileStorageClient.queryFileInfo(DEFAULT_GROUP, path);
        //获取文件大小信息
        long fileSize = fileInfo.getFileSize();
        String url=httpFdfsStorageAddr+path;
        //获取请求头
        Enumeration<String> headerNames = request.getHeaderNames();
        Map<String,Object> headers=new HashMap<>();
        //递归获取到所有的请求头信息 封装到map集合当中
        while (headerNames.hasMoreElements()){
            String header = headerNames.nextElement();
            headers.put(header,request.getHeader(header));
        }
        //获取请求头中的range信息
        String rangeStr = request.getHeader("Range");
        String[] range;
        //判断range是否为空
        if(StringUtil.isNullOrEmpty(rangeStr)){
            rangeStr = "bytes=0-" + (fileSize-1);
        }
        // 将Range:bytes=1572864- 正则切割
        range = rangeStr.split("bytes=|-");
        long begin = 0;
        //如果range长度为2 切割后 应该是 bytes=,1572864 那么后续就要加上 最后end就是文件的大小
        if(range.length >= 2){
            begin = Long.parseLong(range[1]);
        }
        long end = fileSize-1;
        if(range.length >= 3){
            end = Long.parseLong(range[2]);
        }
        long len = (end - begin) + 1;
        //封装响应请求头
        String contentRange = "bytes " + begin + "-" + end + "/" + fileSize;
        response.setHeader("Content-Range", contentRange);
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("Content-Type", "video/mp4");
        response.setContentLength((int)len);
        response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        HttpUtil.get(url, headers, response);
    }





}
