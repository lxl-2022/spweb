package com.immoc.bilibili.service.impl;

import com.immoc.bilibili.dao.AuthRoleDao;
import com.immoc.bilibili.domain.auth.AuthRole;
import com.immoc.bilibili.domain.auth.AuthRoleElementOperation;
import com.immoc.bilibili.domain.auth.AuthRoleMenu;
import com.immoc.bilibili.service.AuthRoleElementOperationService;
import com.immoc.bilibili.service.AuthRoleMenuService;
import com.immoc.bilibili.service.AuthRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class AuthRoleServiceimpl implements AuthRoleService {

    @Autowired
    private AuthRoleDao authRoleDao;

    @Autowired
    private AuthRoleElementOperationService authRoleElementOperationService;

    @Autowired
    private AuthRoleMenuService authRoleMenuService;


    @Override
    public List<AuthRoleElementOperation> getRoleElementOperationsByRoleIds(Set<Long> roleIdSet) {
        return authRoleElementOperationService.getRoleElementOperationsByRoleIds(roleIdSet);
    }

    @Override
    public List<AuthRoleMenu> getAuthRoleMenusByRoleIds(Set<Long> roleIdSet) {
        return authRoleMenuService.getAuthRoleMenusByRoleIds(roleIdSet);
    }

    @Override
    public AuthRole getRoleBycode(String code) {
        return authRoleDao.getRoleByCode(code);
    }
}
