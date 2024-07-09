package com.immoc.bilibili.dao;


import com.alibaba.fastjson.JSONObject;
import com.immoc.bilibili.domain.RefreshTokenDetail;
import com.immoc.bilibili.domain.User;
import com.immoc.bilibili.domain.UserInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mapper
@Component
public interface bilibiliDao {
    User getUserbyPhone(String phone);

    Integer adduser(User user);


    Integer addUserInfo(UserInfo userInfo);

    User getUserById(Long id);

    UserInfo getUserInforById(Long id);

    Integer updateUserInfos(UserInfo userInfo);

    List<UserInfo> getUserInfoByUserIds(Set<Long> followingIdSet);

    Integer pageCountUserInfos(Map<String,Object> params);

    List<UserInfo> pageListUserInfos(Map<String, Object> params);

    Integer deleteRefreshToken(@Param("refreshToken") String refreshToken,
                               @Param("userId") Long userId);

    Integer addRefreshToken(@Param("refreshToken")String refreshToken,
                            @Param("userId") Long userId,
                            @Param("createTime") Date createTime);

    void deleteRefreshTokenByUserId(Long userId);

    RefreshTokenDetail getRefreshTokenDetail(String refreshToken);

    List<UserInfo> batchGetUserInfoByUserIds(@Param("userIdList") Set<Long> userIdList);
}
