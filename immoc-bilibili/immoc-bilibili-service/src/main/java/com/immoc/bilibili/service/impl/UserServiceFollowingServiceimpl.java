package com.immoc.bilibili.service.impl;

import com.immoc.bilibili.constant.UserConstant;
import com.immoc.bilibili.dao.UserFollowingDao;
import com.immoc.bilibili.domain.FollowingGroup;
import com.immoc.bilibili.domain.User;
import com.immoc.bilibili.domain.UserFollowing;
import com.immoc.bilibili.domain.UserInfo;
import com.immoc.bilibili.domain.exception.ConditionException;
import com.immoc.bilibili.service.UserServiceFollowingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserServiceFollowingServiceimpl implements UserServiceFollowingService {

    @Autowired
    private UserFollowingDao userFollowingDao;

    @Autowired
    private FllowingGropuServiceimpl followingGroupService;

    @Autowired
    private biliiliserviceimpl userService;

    //添加关注信息
    @Transactional
    public void addUserFollowings(UserFollowing userFollowing) {
        //首先获取关注用户的信息表中的分组id
        Long groupId = userFollowing.getGroupId();
        //判断分组id是否为空 是空的话 就采用默认分组
        if(groupId == null){
            FollowingGroup followingGroup = followingGroupService.getByType(UserConstant.USER_FOLLOWING_GROUP_TYPE_DEFAULT);
            userFollowing.setGroupId(followingGroup.getId());
        }else{
            FollowingGroup followingGroup = followingGroupService.getById(groupId);
            if(followingGroup == null){
                throw new ConditionException("关注分组不存在！");
            }
        }
        //获取关注用户id
        Long followingId = userFollowing.getFollowingId();
        //查找关注的用户信息
        User user = userService.getUserById(followingId);
        if(user == null){
            throw new ConditionException("关注的用户不存在！");
        }
        //这其实是一个更新操作 先删除在新增
        userFollowingDao.deleteUserFollowing(userFollowing.getUserId(), followingId);
        userFollowing.setCreateTime(new Date());
        System.out.println(userFollowing);
        userFollowingDao.addUserFollowing(userFollowing);
    }

    // 第一步：获取关注的用户列表
    // 第二步：根据关注用户的id查询关注用户的基本信息
    // 第三步：将关注用户按关注分组进行分类
    public List<FollowingGroup> getUserFollowings(Long userId){
        //根据userid(用户id) 查询出 关注的所有信息
        List<UserFollowing> list=userFollowingDao.getUserFollowings(userId);
        // 将当前用户所有关注用户id 抽取出来
        Set<Long> followingIdSet = list.stream().map(UserFollowing::getFollowingId).collect(Collectors.toSet());
        List<UserInfo> userInfoList = new ArrayList<>();
        //查询当前用户关注的用户信息集合
        if(followingIdSet.size() > 0){
            userInfoList = userService.getUserInfoByUserIds(followingIdSet);
        }
        //判断关注的用户Id与 查询当前关注用户信息的id是否一样 一样的话添加到关注列表当中
        for(UserFollowing userFollowing : list){
            for(UserInfo userInfo : userInfoList){
                if(userFollowing.getFollowingId().equals(userInfo.getUserId())){
                    userFollowing.setUserInfo(userInfo);
                }
            }
        }
        // 根据用户id查询他的关注分组信息
        List<FollowingGroup> groupList = followingGroupService.getByUserId(userId);
        // 给前端显示添加全部关注
        FollowingGroup allGroup = new FollowingGroup();
        allGroup.setName(UserConstant.USER_FOLLOWING_GROUP_ALL_NAME);
        allGroup.setFollowingUserInfoList(userInfoList);
        List<FollowingGroup> result = new ArrayList<>();
        result.add(allGroup);
        //将关注用户分组中的每一个关注用户信息存储在infoList中
        for(FollowingGroup group : groupList){
            List<UserInfo> infoList = new ArrayList<>();
            for(UserFollowing userFollowing : list){
                if(group.getId().equals(userFollowing.getGroupId())){
                    infoList.add(userFollowing.getUserInfo());
                }

            }
            group.setFollowingUserInfoList(infoList);
            result.add(group);
        }
        return result;
    }

    // 第一步：获取当前用户的粉丝列表
    // 第二步：根据粉丝的用户id查询基本信息
    // 第三步：查询当前用户是否已经关注该粉丝
    public List<UserFollowing> getUserFans(Long userId){
        // 查出当前用户的粉丝
        List<UserFollowing> fanList = userFollowingDao.getUserFans(userId);
        //用lamda表达式 抽取粉丝的id 并且查询粉丝的相关信息
        Set<Long> fanIdSet = fanList.stream().map(UserFollowing::getUserId).collect(Collectors.toSet());
        List<UserInfo> userInfoList = new ArrayList<>();
        if(fanIdSet.size() > 0){
            userInfoList = userService.getUserInfoByUserIds(fanIdSet);
        }

        //获取当前用户的信息
        List<UserFollowing> followingList = userFollowingDao.getUserFollowings(userId);
        for(UserFollowing fan : fanList){
            for(UserInfo userInfo : userInfoList){
                if(fan.getUserId().equals(userInfo.getUserId())){
                    userInfo.setFollowed(false);
                    fan.setUserInfo(userInfo);
                }
            }
            //判断当前用户与粉丝是否互相关注
            for(UserFollowing following : followingList){
                if(following.getFollowingId().equals(fan.getUserId())){
                    fan.getUserInfo().setFollowed(true);
                }
            }
        }
        return fanList;
    }

    @Override
    public Long addUserFollowingGroups(FollowingGroup followingGroup) {
        followingGroup.setCreateTime(new Date());
        followingGroup.setType(UserConstant.USER_FOLLOWING_GROUP_TYPE_USER);
        followingGroupService.addFollowingGroup(followingGroup);
        return followingGroup.getId();
    }

    @Override
    public List<FollowingGroup> getUserFollowingGroups(Long userId) {
        return followingGroupService.getUserFollowingGroups(userId);
    }

    @Override
    public List<UserInfo> checkFollowingStatus(List<UserInfo> list, Long userId) {
        List<UserFollowing> userFollowingList = userFollowingDao.getUserFollowings(userId);
        for(UserInfo userInfo : list){
            userInfo.setFollowed(false);
            for(UserFollowing userFollowing : userFollowingList){
                if(userFollowing.getFollowingId().equals(userInfo.getUserId())){
                    userInfo.setFollowed(true);
                }
            }
        }
        return list;
    }


}
