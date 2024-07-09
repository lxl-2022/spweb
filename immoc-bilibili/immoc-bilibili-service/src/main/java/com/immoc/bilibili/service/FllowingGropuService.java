package com.immoc.bilibili.service;

import com.immoc.bilibili.domain.FollowingGroup;

import java.util.List;

public interface FllowingGropuService {

    public FollowingGroup getByType(String Type);

    public FollowingGroup getById(Long id);

    public List<FollowingGroup> getByUserId(Long userId);
}
