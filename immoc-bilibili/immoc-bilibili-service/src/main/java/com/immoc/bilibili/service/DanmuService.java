package com.immoc.bilibili.service;

import com.immoc.bilibili.domain.Danmu;

import java.text.ParseException;
import java.util.List;

public interface DanmuService {
    List<Danmu> getDanmus(Long videoId, String startTime, String endTime) throws ParseException;

    void addDanmusToRedis(Danmu danmu);

    void asynaddDanmu(Danmu danmu);

    void addDanmu(Danmu danmu);
}
