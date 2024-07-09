package com.immoc.Aspect;

import com.immoc.bilibili.constant.AuthRoleConstant;
import com.immoc.bilibili.domain.UserMoment;
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
public class DateLimitRoleAspect {

    @Autowired
    private UserSupport userSupport;

    @Autowired
    private UserRoleService userRoleService;

    //切点 表示使用该注解时候 会织入切点
    @Pointcut("@annotation(com.immoc.bilibili.domain.annotation.DataLimited)")
    public  void check(){

    }

    //判断参数
    @Before("check()")
    public  void dobefor(JoinPoint joinPoint){
        Long userId = userSupport.getCurrentUserId();
        List<UserRole> userRoleList = userRoleService.getUserRoleByUserId(userId);

        //获取当前的角色编码
        Set<String> roleCodeSet=userRoleList.stream().map(UserRole::getRoleCode).collect(Collectors.toSet());
        Object[] args = joinPoint.getArgs(); //切面获得到的所有参数
        for (Object arg : args) {
            if(arg instanceof UserMoment){
                UserMoment userMoment=(UserMoment) arg;
                String type = userMoment.getType();
                if(roleCodeSet.contains(AuthRoleConstant.ROLE_LV0) && !"0".equals(type)){
                    throw new ConditionException("参数与等级不匹配");
                }
            }
        }
    }
}
