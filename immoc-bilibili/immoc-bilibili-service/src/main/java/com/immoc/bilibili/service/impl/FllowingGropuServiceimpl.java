package com.immoc.bilibili.service.impl;

import com.immoc.bilibili.dao.FollowingGroupDao;
import com.immoc.bilibili.domain.FollowingGroup;
import com.immoc.bilibili.service.FllowingGropuService;
import com.immoc.bilibili.service.UserServiceFollowingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FllowingGropuServiceimpl implements FllowingGropuService {

    @Autowired
    private FollowingGroupDao followingGroupDao;

    //根据类型查询分组
    public FollowingGroup getByType(String Type){
        return followingGroupDao.getByType(Type);
    }

    //根据id查询分组
    public FollowingGroup getById(Long id){
        return followingGroupDao.getById(id);
    }

    public List<FollowingGroup> getByUserId(Long userId) {
        return followingGroupDao.getByUserId(userId);
    }

    public void addFollowingGroup(FollowingGroup followingGroup) {
        followingGroupDao.addFollowingGroup(followingGroup);
    }


    public List<FollowingGroup> getUserFollowingGroups(Long userId) {
        return followingGroupDao.getUserFollowingGroups(userId);
    }
}
