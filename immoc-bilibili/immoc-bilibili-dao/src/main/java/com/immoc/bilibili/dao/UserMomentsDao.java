package com.immoc.bilibili.dao;

import com.immoc.bilibili.domain.UserMoment;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Mapper
@Component
public interface UserMomentsDao {

    Integer addUserMoments(UserMoment userMoment);
}
