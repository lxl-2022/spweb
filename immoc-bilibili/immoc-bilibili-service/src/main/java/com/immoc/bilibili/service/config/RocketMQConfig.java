package com.immoc.bilibili.service.config;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.immoc.bilibili.constant.UserMomentsConstant;
import com.immoc.bilibili.domain.UserFollowing;
import com.immoc.bilibili.domain.UserMoment;
import com.immoc.bilibili.service.UserServiceFollowingService;
import com.immoc.bilibili.service.WebSocket.WebSocketService;
import com.mysql.cj.xdevapi.JsonArray;
import io.netty.util.internal.StringUtil;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class RocketMQConfig {

    @Value("${rocketmq.name.server.address}")
    private  String nameServerAddr;

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    @Autowired
    private UserServiceFollowingService userServiceFollowingService;

    //创建生产者
    @Bean("momentsProducer")
    public DefaultMQProducer momentsProducer() throws MQClientException {
        //设置名称
        DefaultMQProducer producer=new DefaultMQProducer(UserMomentsConstant.GROUP_MOMENTS);
        //设置地址
        producer.setNamesrvAddr(nameServerAddr);
        producer.start();
        return  producer;
    }

    //创建消费者 推送方式中间件实现消息传递
    @Bean("momentConsumer")
    public DefaultMQPushConsumer momentConsumer() throws MQClientException {
        DefaultMQPushConsumer consumer=new DefaultMQPushConsumer(UserMomentsConstant.GROUP_MOMENTS);
        consumer.setNamesrvAddr(nameServerAddr);
        //订阅 topic 模式 订阅topic，可以对指定消息进行过滤，例如："TopicTest","tagl||tag2||tag3",*或null表示topic所有消息
        consumer.subscribe(UserMomentsConstant.TOPIC_MOMENTS,"*");
        // 注册消息监听器
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> list, ConsumeConcurrentlyContext consumeConcurrentlyContext) {
                //当前列表一般只有一个消息所以
                MessageExt msg = list.get(0);
                // 当前获取的信息是Json数据格式需要将Json转换成Pojo类型
                byte[] msgByte = msg.getBody();
                String bodystr = new String(msgByte);
                JSONObject jsonObject = JSONObject.parseObject(bodystr);
                UserMoment userMoment = JSONObject.toJavaObject(jsonObject, UserMoment.class);
                Long userId = userMoment.getUserId();
                //获取到userId以后 需要进行判断哪些粉丝关注了当前的用户
                List<UserFollowing> userFans = userServiceFollowingService.getUserFans(userId);
                //需要给redis中存入 推送接受到的数据
                for (UserFollowing fan : userFans) {
                    String key="subscribed"+fan.getUserId();
                    String subscribedListStr = redisTemplate.opsForValue().get(key);
                    List<UserMoment> subscribedList;
                    //判断redis中的key查到的数据是否为空
                    if(StringUtil.isNullOrEmpty(subscribedListStr)){
                        //如果为空就新建一个列表
                        subscribedList =new ArrayList<>();
                    }else {
                        //如果有数据就将redis数据从JSON形式转换成列表形式
                        subscribedList=JSONArray.parseArray(subscribedListStr,UserMoment.class);
                    }
                    subscribedList.add(userMoment);
                    redisTemplate.opsForValue().set(key,JSONObject.toJSONString(subscribedList));

                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;

            }
        });
        consumer.start();
        return consumer;
    }

    //创建弹幕消息队列 生产者
    @Bean("danmusProducer")
    public DefaultMQProducer danmusProducer() throws Exception{
        // 实例化消息生产者Producer
        DefaultMQProducer producer = new DefaultMQProducer(UserMomentsConstant.GROUP_DANMUS);
        // 设置NameServer的地址
        producer.setNamesrvAddr(nameServerAddr);
        // 启动Producer实例
        producer.start();
        return producer;
    }

    //消息队列的消费者
    @Bean("danmusConsumer")
    public DefaultMQPushConsumer danmusConsumer() throws MQClientException {
        //实例化消费者
        DefaultMQPushConsumer consumer=new DefaultMQPushConsumer(UserMomentsConstant.GROUP_DANMUS);
        //设置nameserver
        consumer.setNamesrvAddr(nameServerAddr);
        //订阅一个或者多个Topic
        consumer.subscribe(UserMomentsConstant.TOPIC_DANMUS,"*");
        //注册回调 实现从消息队列拉取信息
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                MessageExt msg = msgs.get(0);
                byte[] msgByte = msg.getBody();
                String byteStr=new String(msgByte);
                JSONObject jsonObject = JSONObject.parseObject(byteStr);
                //将生产者发送的消息提取出来
                String sessionId = jsonObject.getString("sessionId");
                String message = jsonObject.getString("message");
                WebSocketService webSocketService = WebSocketService.WEBSOCKET_MAP.get(sessionId);
                if(webSocketService.getSession().isOpen()){
                    try {
                        webSocketService.sendMessage(message);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                // 标记该消息已经被成功消费
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        // 启动消费者实例
        consumer.start();
        return consumer;
    }
}
