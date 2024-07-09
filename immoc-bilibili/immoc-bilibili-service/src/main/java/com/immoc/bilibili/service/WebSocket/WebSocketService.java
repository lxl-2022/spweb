package com.immoc.bilibili.service.WebSocket;


import com.alibaba.fastjson.JSONObject;
import com.immoc.bilibili.constant.UserMomentsConstant;
import com.immoc.bilibili.domain.Danmu;
import com.immoc.bilibili.service.DanmuService;
import com.immoc.bilibili.service.util.RockeMQUtil;
import com.immoc.bilibili.service.util.TokenUtil;
import io.netty.util.internal.StringUtil;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@ServerEndpoint("/imserver/{token}")
public class WebSocketService {

    //日志记录
    private final Logger logger= LoggerFactory.getLogger(this.getClass());

    //在线人数 使用原子性操作 保证线程安全
    private static  final AtomicInteger online_count=new AtomicInteger(0);

    //每一个客户端关联的websocket
    public static final ConcurrentHashMap<String,WebSocketService> WEBSOCKET_MAP=new ConcurrentHashMap<>();

    //websocket中具有session
    private Session session;

    private String sessionId;


    //当前webscoket的用户id
    private Long userId;

    //针对websocket多例模式下无法使用springboot单例注入 采用的替代方式
    private static ApplicationContext applicationContext;
    public static void setApplicationContext(ApplicationContext applicationContext){
        WebSocketService.applicationContext=applicationContext;
    }

    //当链接成功以后需要调用该方法
    @OnOpen
    public void openConnection(Session session,@PathParam("token") String token){
        //根据Token来验证当前的用户id
        try{
            this.userId = TokenUtil.verifyToken(token);
        }catch (Exception ignored){}
        this.sessionId=session.getId();
        this.session=session;
        if(WEBSOCKET_MAP.contains(sessionId)){
            //删除原来的websocket 加入当前的websocket
            WEBSOCKET_MAP.remove(sessionId);
            WEBSOCKET_MAP.put(sessionId,this);
        }else {
            WEBSOCKET_MAP.put(sessionId,this);
            //在线人数+1
            online_count.getAndIncrement();
        }
        logger.info("用户链接成功"+sessionId+"当前在线人数为"+","+online_count.get());
        try {
            //告诉前端链接成功
            this.sendMessage("链接成功");
        }catch (Exception e){
            logger.info("链接异常");
            System.out.println(e.getMessage());
        }
    }

    //当链接断开时候访问
    @OnClose
    public void closeConnection(){
        if(WEBSOCKET_MAP.containsKey(sessionId)){
            WEBSOCKET_MAP.remove(sessionId);
            online_count.getAndDecrement();
        }
        logger.info("用户退出"+sessionId+"当前在线人数为"+","+online_count.get());
    }

    //前端给后端发送消息时候调用
    @OnMessage
    public void onMessage(String message){
        logger.info("用户信息：" + sessionId + "，报文：" + message);
        //如果当前的数据不为空
        if(StringUtil.isNullOrEmpty(message)){
            try {
                /**将当前的弹幕信息通过websocket在传回所有在线用户界面
                获取所有的websocket
                 **/
                for (Map.Entry<String, WebSocketService> entry : WEBSOCKET_MAP.entrySet()) {
                    WebSocketService webSocketService = entry.getValue();
                    /**
                     * 在高并发场景下采用MQ+异步 实现流量削峰
                     */
                    DefaultMQProducer danmusProducer =(DefaultMQProducer)applicationContext.getBean("danmusProducer");
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("message",message);
                    jsonObject.put("sessionId",webSocketService.getSessionId());
                    Message msg = new Message(UserMomentsConstant.TOPIC_DANMUS, jsonObject.toJSONString().getBytes(StandardCharsets.UTF_8));
                    RockeMQUtil.syncSendMessage(danmusProducer, msg);
                }
                //只有注册了的用户发的弹幕才会进行保存
                if(this.userId!=null){
                    //保存弹幕到数据库
                    Danmu danmu = JSONObject.parseObject(message, Danmu.class);
                    danmu.setUserId(userId);
                    danmu.setCreateTime(new Date());
                    DanmuService danmuService = (DanmuService)applicationContext.getBean("DanmuService");
                    //异步保存到数据库当中
                    danmuService.asynaddDanmu(danmu);
                    //保存弹幕到redis
                    danmuService.addDanmusToRedis(danmu);
                }
            }catch (Exception e){
                logger.error("弹幕接收出现问题");
                e.printStackTrace();
            }
        }
    }

    //定时向前端推送当前在线人数
    @Scheduled(fixedRate = 5000)
    public void noticeOnliineCount() throws IOException {
        for (Map.Entry<String, WebSocketService> entry : WebSocketService.WEBSOCKET_MAP.entrySet()) {
            WebSocketService webSocketService = entry.getValue();
            if(webSocketService.session.isOpen()){
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("onlineCount", online_count.get());
                jsonObject.put("msg", "当前在线人数为" + online_count.get());
                webSocketService.sendMessage(jsonObject.toJSONString());
            }
        }
    }

    @OnError
    public void onError(Throwable error){

    }

    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }

    public Session getSession() {
        return session;
    }

    public String getSessionId() {
        return sessionId;
    }
}
