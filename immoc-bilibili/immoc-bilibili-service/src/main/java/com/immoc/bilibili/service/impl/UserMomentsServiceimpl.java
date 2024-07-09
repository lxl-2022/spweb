package com.immoc.bilibili.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.immoc.bilibili.constant.UserMomentsConstant;
import com.immoc.bilibili.dao.UserMomentsDao;
import com.immoc.bilibili.domain.UserMoment;
import com.immoc.bilibili.service.UserMomentsService;
import com.immoc.bilibili.service.util.RockeMQUtil;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Service
public class UserMomentsServiceimpl implements UserMomentsService {


    @Autowired
    private UserMomentsDao userMomentsDao;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    @Override
    public void addUserMoments(UserMoment userMoment) throws InterruptedException, RemotingException, MQClientException, MQBrokerException {
        userMoment.setCreateTime(new Date());
        userMomentsDao.addUserMoments(userMoment);
        //生产者像Mq当中传数据
        DefaultMQProducer producer = (DefaultMQProducer)applicationContext.getBean("momentsProducer");
        /**
         *  参数一：topic的名称 与消费者对应
         *  参数二：消息队列中的消息
         *     public Message(String topic, byte[] body) {
         *         this(topic, "", "", 0, body, true);
         *     }
         */
        Message msg = new Message(UserMomentsConstant.TOPIC_MOMENTS, JSONObject.toJSONString(userMoment).getBytes(StandardCharsets.UTF_8));
        RockeMQUtil.syncSendMessage(producer,msg);

    }

    @Override
    public List<UserMoment> getUserSubscribedMoments(Long userId) {
        String key="subscribed"+userId;
        String value=redisTemplate.opsForValue().get(key);
        return JSONArray.parseArray(value,UserMoment.class);
    }
}
