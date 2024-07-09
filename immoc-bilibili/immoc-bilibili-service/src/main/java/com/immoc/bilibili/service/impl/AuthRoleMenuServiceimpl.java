package com.immoc.bilibili.service.impl;


import com.immoc.bilibili.dao.AuthRoleMenuDao;
import com.immoc.bilibili.domain.auth.AuthRoleMenu;
import com.immoc.bilibili.service.AuthRoleMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class AuthRoleMenuServiceimpl implements AuthRoleMenuService {

    @Autowired
    private AuthRoleMenuDao authRoleMenuDao;

    @Override
    public List<AuthRoleMenu> getAuthRoleMenusByRoleIds(Set<Long> roleIdSet) {
        return authRoleMenuDao.getAuthRoleMenusByRoleIds(roleIdSet);
    }
}
