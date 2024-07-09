package com.immoc.controller;


import com.immoc.bilibili.constant.AuthRoleConstant;
import com.immoc.bilibili.domain.JsonResponse;
import com.immoc.bilibili.domain.UserMoment;
import com.immoc.bilibili.domain.annotation.ApiLimitedRole;
import com.immoc.bilibili.domain.annotation.DataLimited;
import com.immoc.bilibili.service.UserMomentsService;
import com.immoc.support.UserSupport;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class UserMomentController {

    @Autowired
    private UserMomentsService userMomentsService;

    @Autowired
    private UserSupport userSupport;

    //新增用户发布动态
    @ApiLimitedRole(limitedRoleCodeList = {AuthRoleConstant.ROLE_LV0})  //等级为0不具备发布动态 这里采用AOP切面注入
    @DataLimited  //判断参数 数据是否与等级匹配
    @PostMapping("/user-moments")
    public JsonResponse<String> addUserMoments(@RequestBody UserMoment userMoment) throws InterruptedException, RemotingException, MQClientException, MQBrokerException {
        Long userId = userSupport.getCurrentUserId();
        userMoment.setUserId(userId);
        userMomentsService.addUserMoments(userMoment);
        return JsonResponse.success();
    }

    //查询用户动态
    @GetMapping("/user-subscribed-moments")
    public  JsonResponse<List<UserMoment>> getUserSubscribedMoment(){
        Long userId = userSupport.getCurrentUserId();
        List<UserMoment> list=userMomentsService.getUserSubscribedMoments(userId);
        return new JsonResponse<>(list);
    }

}
