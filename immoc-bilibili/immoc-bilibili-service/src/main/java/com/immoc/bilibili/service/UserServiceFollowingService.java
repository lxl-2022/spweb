package com.immoc.bilibili.service;

import com.immoc.bilibili.domain.FollowingGroup;
import com.immoc.bilibili.domain.UserFollowing;
import com.immoc.bilibili.domain.UserInfo;

import java.util.List;

public interface UserServiceFollowingService {

    public void addUserFollowings(UserFollowing userFollowing);

    public List<FollowingGroup> getUserFollowings(Long userId);

    public List<UserFollowing> getUserFans(Long userId);

    Long addUserFollowingGroups(FollowingGroup followingGroup);

    List<FollowingGroup> getUserFollowingGroups(Long userId);

    List<UserInfo> checkFollowingStatus(List<UserInfo> list, Long userId);

}
