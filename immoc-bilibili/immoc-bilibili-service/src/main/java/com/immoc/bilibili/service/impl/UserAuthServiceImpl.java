package com.immoc.bilibili.service.impl;

import com.immoc.bilibili.constant.AuthRoleConstant;
import com.immoc.bilibili.domain.auth.*;
import com.immoc.bilibili.service.AuthRoleService;
import com.immoc.bilibili.service.UserAuthService;
import com.immoc.bilibili.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserAuthServiceImpl implements UserAuthService {

    //用户角色关联
    @Autowired
    private UserRoleService userRoleService;

    //角色权限
    @Autowired
    private AuthRoleService authRoleService;

    @Override
    public UserAuthorities getUserAuthorities(Long userId) {
        //根据用户id查询用户与角色的关联信息
        List<UserRole> userRoleList = userRoleService.getUserRoleByUserId(userId);
        //将角色id获取出来
        Set<Long> roleIdSet = userRoleList.stream().map(UserRole :: getRoleId).collect(Collectors.toSet());
        //根据角色id 查询权限控制信息
        List<AuthRoleElementOperation> roleElementOperationList = authRoleService.getRoleElementOperationsByRoleIds(roleIdSet);
        //根据角色id 查询菜单权限信息
        List<AuthRoleMenu> authRoleMenuList = authRoleService.getAuthRoleMenusByRoleIds(roleIdSet);

        //将当前id相关的角色，权限信息都查询出来存储在了UserAuthorities的pojo中了
        UserAuthorities userAuthorities = new UserAuthorities();
        userAuthorities.setRoleElementOperationList(roleElementOperationList);
        userAuthorities.setRoleMenuList(authRoleMenuList);
        return userAuthorities;

    }

    @Override
    public void addUserDefaultRole(Long id) {
        UserRole userRole=new UserRole();
        //为创建的用户添加LV0的用户
        AuthRole role=authRoleService.getRoleBycode(AuthRoleConstant.ROLE_LV0);
        userRole.setId(id);
        userRole.setRoleId(role.getId());
        userRoleService.addUserRole(userRole);
    }
}
