package com.immoc.bilibili.service;

import com.immoc.bilibili.domain.UserMoment;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.remoting.exception.RemotingException;

import java.util.List;

public interface UserMomentsService {
    void addUserMoments(UserMoment userMoment) throws InterruptedException, RemotingException, MQClientException, MQBrokerException;

    List<UserMoment> getUserSubscribedMoments(Long userId);
}
