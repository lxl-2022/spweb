package com.immoc.bilibili.service.util;

import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.CountDownLatch2;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;

import java.util.concurrent.TimeUnit;

public class RockeMQUtil {

    //消息队列同步发送
    public static void syncSendMessage(DefaultMQProducer producer, Message msg) throws InterruptedException, RemotingException, MQClientException, MQBrokerException {
        SendResult send = producer.send(msg);
        System.out.println(msg);
    }

    //消息队列异步发送
    public static void asyncSendMessage(DefaultMQProducer producer,Message msg)throws Exception{
        int messageConut=2;
        CountDownLatch2 countDownLatch2 = new CountDownLatch2(messageConut);
        for(int i=0;i<messageConut;i++) {
            producer.send(msg, new SendCallback() {
                //异步发送时候需要借助countDownLunch
                @Override
                public void onSuccess(SendResult sendResult) {
                    countDownLatch2.countDown();
                    System.out.println(sendResult.getMsgId());
                }

                @Override
                public void onException(Throwable throwable) {
                    countDownLatch2.countDown();
                    System.out.println("发送出现了异常");
                    throwable.printStackTrace();
                }
            });
        }
        countDownLatch2.await(5, TimeUnit.SECONDS);
    }
}
