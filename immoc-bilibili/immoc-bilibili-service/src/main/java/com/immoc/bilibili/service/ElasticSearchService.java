package com.immoc.bilibili.service;

import com.immoc.bilibili.domain.UserInfo;
import com.immoc.bilibili.domain.Video;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface ElasticSearchService {

    public void addVideo(Video video);

    public Video getVideos(String keyword);

    public void addUserInfo(UserInfo userInfo);

    public List<Map<String, Object>> getContents(String keyword,
                                                 Integer pageNo,
                                                 Integer pageSize)throws IOException;
}
