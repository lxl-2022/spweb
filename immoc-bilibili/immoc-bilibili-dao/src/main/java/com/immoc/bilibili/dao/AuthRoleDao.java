package com.immoc.bilibili.dao;


import com.immoc.bilibili.domain.auth.AuthRole;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

@Mapper
@Component
public interface AuthRoleDao {
    AuthRole getRoleByCode(String code);
}
