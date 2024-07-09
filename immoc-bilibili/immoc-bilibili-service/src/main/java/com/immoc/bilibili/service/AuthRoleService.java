package com.immoc.bilibili.service;

import com.immoc.bilibili.domain.auth.AuthRole;
import com.immoc.bilibili.domain.auth.AuthRoleElementOperation;
import com.immoc.bilibili.domain.auth.AuthRoleMenu;

import java.util.List;
import java.util.Set;

public interface AuthRoleService  {
    List<AuthRoleElementOperation> getRoleElementOperationsByRoleIds(Set<Long> roleIdSet);

    List<AuthRoleMenu> getAuthRoleMenusByRoleIds(Set<Long> roleIdSet);

    AuthRole getRoleBycode(String code);
}
