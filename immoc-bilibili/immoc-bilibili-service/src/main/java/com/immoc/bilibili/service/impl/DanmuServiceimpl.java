package com.immoc.bilibili.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.immoc.bilibili.dao.DanmuDao;
import com.immoc.bilibili.domain.Danmu;
import com.immoc.bilibili.service.DanmuService;
import com.immoc.bilibili.service.WebSocket.WebSocketService;
import io.netty.util.internal.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


@Service
public class DanmuServiceimpl implements DanmuService {

    @Autowired
    private DanmuDao danmuDao;

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    private static final String DANMU_KEY = "dm-video-";

    public void addDanmu(Danmu danmu){
        danmuDao.addDanmu(danmu);
    }

    @Async
    public void asynaddDanmu(Danmu danmu){
        danmuDao.addDanmu(danmu);
    }

    @Override
    public List<Danmu> getDanmus(Long videoId, String startTime, String endTime) throws ParseException {
        String key = DANMU_KEY + videoId;
        String value = redisTemplate.opsForValue().get(key);
        List<Danmu> list;
        //如果redis中有数据就从redis中取
        if(!StringUtil.isNullOrEmpty(value)){
            list = JSONArray.parseArray(value, Danmu.class);
            if(!StringUtil.isNullOrEmpty(startTime)
                    && !StringUtil.isNullOrEmpty(endTime)){
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date startDate = sdf.parse(startTime);
                Date endDate = sdf.parse(endTime);
                List<Danmu> childList = new ArrayList<>();
                for(Danmu danmu : list){
                    //获取所有弹幕列表创建的时间
                    Date createTime = danmu.getCreateTime();
                    //筛选出弹幕创建时间在给定范围内的弹幕
                    if(createTime.after(startDate) && createTime.before(endDate)){
                        childList.add(danmu);
                    }
                }
                list = childList;
            }
        }else{
            //否则从数据库取 按照时间段进行取
            Map<String, Object> params = new HashMap<>();
            params.put("videoId", videoId);
            params.put("startTime", startTime);
            params.put("endTime", endTime);
            list = danmuDao.getDanmus(params);
            //保存弹幕到redis
            redisTemplate.opsForValue().set(key, JSONObject.toJSONString(list));
        }
        return list;

    }

    //将弹幕信息存储到redis当中
    public void addDanmusToRedis(Danmu danmu){
        String key=DANMU_KEY+danmu.getVideoId();
        String value=redisTemplate.opsForValue().get(key);
        List<Danmu> list = new ArrayList<>();
        //将存储的vlue字符串转换成Danmu的Pojo类型
        if(!StringUtil.isNullOrEmpty(value)){
            list = JSONArray.parseArray(value, Danmu.class);
        }
        //将当前的弹幕存储到列表当中
        list.add(danmu);
        redisTemplate.opsForValue().set(key, JSONObject.toJSONString(list));
    }


}
