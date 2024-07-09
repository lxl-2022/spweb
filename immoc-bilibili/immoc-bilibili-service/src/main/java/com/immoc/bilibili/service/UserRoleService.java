package com.immoc.bilibili.service;

import com.immoc.bilibili.domain.auth.UserRole;

import java.util.List;

public interface UserRoleService {
    List<UserRole> getUserRoleByUserId(Long userId);

    void addUserRole(UserRole userRole);
}
