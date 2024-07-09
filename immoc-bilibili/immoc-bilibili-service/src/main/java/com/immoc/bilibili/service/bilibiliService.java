package com.immoc.bilibili.service;

import com.alibaba.fastjson.JSONObject;
import com.immoc.bilibili.domain.PageResult;
import com.immoc.bilibili.domain.User;
import com.immoc.bilibili.domain.UserInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface bilibiliService {


    void adduser(User user);

    User getUserbyPhone(String phone);


    String login(User user);

    User getUserInfo(Long id);

    void updateUserInfos(UserInfo userInfo);

    PageResult<UserInfo> pageListUserInfos(JSONObject params);

    Map<String, Object> loginForDts(User user) throws Exception;

    void logout(String refreshToken, Long userId);

    String refreshAccessToken(String refreshToken) throws Exception;

    List<UserInfo> batchGetUserInfoByUserIds(Set<Long> userIdList);
}
