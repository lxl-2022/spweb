package com.immoc.Aspect;

import com.immoc.bilibili.domain.annotation.ApiLimitedRole;
import com.immoc.bilibili.domain.auth.UserRole;
import com.immoc.bilibili.domain.exception.ConditionException;
import com.immoc.bilibili.service.UserRoleService;
import com.immoc.support.UserSupport;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Order(1)
@Aspect  //注解切面编程
@Component
public class LimitRoleAspect {

    @Autowired
    private UserSupport userSupport;

    @Autowired
    private UserRoleService userRoleService;

    //切点 表示使用该注解时候 会织入切点
    @Pointcut("@annotation(com.immoc.bilibili.domain.annotation.ApiLimitedRole)")
    public  void check(){

    }

    @Before("check() && @annotation(apiLimitedRole)")
    public  void dobefor(JoinPoint joinPoint, ApiLimitedRole apiLimitedRole){
        Long userId = userSupport.getCurrentUserId();
        List<UserRole> userRoleList = userRoleService.getUserRoleByUserId(userId);
        String[] roleCodeList = apiLimitedRole.limitedRoleCodeList();
        //获取不能被调用接口的角色编码
        Set<String> limitset= Arrays.stream(roleCodeList).collect(Collectors.toSet());
        //获取当前的角色编码
        Set<String> roleCodeSet=userRoleList.stream().map(UserRole::getRoleCode).collect(Collectors.toSet());
        roleCodeSet.retainAll(limitset); //取了两个set的交集
        if(roleCodeSet.size()>0){
            throw new ConditionException("权限不足！！");
        }
    }
}
