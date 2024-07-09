package com.immoc.bilibili.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.immoc.bilibili.constant.UserConstant;
import com.immoc.bilibili.dao.bilibiliDao;
import com.immoc.bilibili.domain.PageResult;
import com.immoc.bilibili.domain.RefreshTokenDetail;
import com.immoc.bilibili.domain.User;
import com.immoc.bilibili.domain.UserInfo;
import com.immoc.bilibili.domain.exception.ConditionException;
import com.immoc.bilibili.service.ElasticSearchService;
import com.immoc.bilibili.service.UserAuthService;
import com.immoc.bilibili.service.bilibiliService;
import com.immoc.bilibili.service.util.MD5Util;
import com.immoc.bilibili.service.util.RSAUtil;

import com.immoc.bilibili.service.util.TokenUtil;
import com.mysql.cj.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class biliiliserviceimpl implements bilibiliService {

    @Autowired
    private bilibiliDao userDao;

    @Autowired
    private UserAuthService userAuthService;


    @Autowired
    private ElasticSearchService elasticSearchService;

    @Override
    public void adduser(User user) {
        //获取手机号
        String phone = user.getPhone();
        if(StringUtils.isNullOrEmpty(phone)){
            throw new ConditionException("手机号不能为空！");
        }
        User dbuser = this.getUserbyPhone(phone);
        if(dbuser!=null){
            throw new ConditionException("该手机号用户已经被注册");
        }
        Date date = new Date();
        //生成盐值 为后续md5加密做准备
        String salt = String.valueOf(date.getTime());
        String password=user.getPassword();
        String rawPassword;
        try {
            rawPassword=RSAUtil.decrypt(password);
        } catch (Exception e) {
           throw new ConditionException("解码失败");
        }
        String md5Password = MD5Util.sign(rawPassword, salt, "UTF-8");
        user.setPassword(md5Password);
        user.setSalt(salt);
        user.setCreateTime(date);
        userDao.adduser(user);
        //添加用户信息
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(user.getId());
        userInfo.setBirth(UserConstant.DEFAULT_BIRTH);
        userInfo.setNick(UserConstant.DEFAULT_NICK);
        userInfo.setGender(UserConstant.GENDER_MALE);
        userInfo.setCreateTime(date);
        userDao.addUserInfo(userInfo);
        //添加用户默认权限角色
        userAuthService.addUserDefaultRole(user.getId());
        //同步数据到es
        elasticSearchService.addUserInfo(userInfo);

    }

    @Override
    public User getUserbyPhone(String phone) {
        User user = userDao.getUserbyPhone(phone);
        return user;
    }

    @Override
    public String login(User user) {
        String phone = user.getPhone();
        if(StringUtils.isNullOrEmpty(phone)){
            throw new ConditionException("手机号不能为空！");
        }
        User dbuser = this.getUserbyPhone(phone);
        if(dbuser==null){
            throw new ConditionException("该用户不存在");
        }

        String password = user.getPassword();
        System.out.println(password);
        String rawpassword=null;
        try {
            rawpassword=RSAUtil.decrypt(password);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String salt = dbuser.getSalt();
        System.out.println(salt);
        System.out.println(rawpassword);
        String md5password = MD5Util.sign(rawpassword, salt, "UTF-8");
        if(!md5password.equals(dbuser.getPassword())){
            throw new ConditionException("密码错误");
        }
        String token;
        try {
            token = TokenUtil.generateToken(dbuser.getId());
        } catch (Exception e) {
            throw new ConditionException("jwt失败");
        }
        return token;
    }

    @Override
    public User getUserInfo(Long id) {
        User user=userDao.getUserById(id);
        UserInfo userinfo = userDao.getUserInforById(id);
        user.setUserInfo(userinfo);
        return user;
    }

    @Override
    public void updateUserInfos(UserInfo userInfo) {
        userInfo.setUpdateTime(new Date());
        userDao.updateUserInfos(userInfo);
    }

    @Override
    public PageResult<UserInfo> pageListUserInfos(JSONObject params) {
        Integer no = params.getInteger("no");
        Integer size = params.getInteger("size");
        params.put("start", (no-1)*size);
        params.put("limit", size);
        //查询是否有当前匹配的nike昵称的数据
        Integer total = userDao.pageCountUserInfos(params);
        List<UserInfo> list = new ArrayList<>();
        //如果查询的nike昵称数据粉丝个数大于0
        if(total > 0){
            //获取粉丝的信息
            list = userDao.pageListUserInfos(params);
        }
        return new PageResult<>(total, list);
    }

    @Override
    public Map<String, Object> loginForDts(User user) throws Exception {
        String phone = user.getPhone() == null ? "" : user.getPhone();
        String email = user.getEmail() == null ? "" : user.getEmail();
        if(StringUtils.isNullOrEmpty(phone) && StringUtils.isNullOrEmpty(email)){
            throw new ConditionException("参数异常！");
        }
        User dbUser = userDao.getUserbyPhone(phone);
        if(dbUser == null){
            throw new ConditionException("当前用户不存在！");
        }
        String password = user.getPassword();
        String rawPassword;
        try{
            rawPassword = RSAUtil.decrypt(password);
        }catch (Exception e){
            throw new ConditionException("密码解密失败！");
        }
        String salt = dbUser.getSalt();
        String md5Password = MD5Util.sign(rawPassword, salt, "UTF-8");
        if(!md5Password.equals(dbUser.getPassword())){
            throw new ConditionException("密码错误！");
        }
        Long userId = dbUser.getId();
        String accessToken = TokenUtil.generateToken(userId);
        String refreshToken = TokenUtil.generateRefreshToken(userId);
        //保存refresh token到数据库
        userDao.deleteRefreshTokenByUserId(userId);
        userDao.addRefreshToken(refreshToken, userId, new Date());
        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", accessToken);
        result.put("refreshToken", refreshToken);
        return result;
    }

    @Override
    public void logout(String refreshToken, Long userId) {
        userDao.deleteRefreshToken(refreshToken, userId);
    }

    @Override
    public String refreshAccessToken(String refreshToken) throws Exception {
        RefreshTokenDetail refreshTokenDetail = userDao.getRefreshTokenDetail(refreshToken);
        if(refreshTokenDetail == null){
            throw new ConditionException("555","token过期！");
        }
        Long userId = refreshTokenDetail.getUserId();
        return TokenUtil.generateToken(userId);
    }

    @Override
    public List<UserInfo> batchGetUserInfoByUserIds(Set<Long> userIdList) {
        return userDao.batchGetUserInfoByUserIds(userIdList);
    }

    public User getUserById(Long followingId) {
        return userDao.getUserById(followingId);
    }

    public List<UserInfo> getUserInfoByUserIds(Set<Long> followingIdSet) {
        return userDao.getUserInfoByUserIds(followingIdSet);
    }
}
