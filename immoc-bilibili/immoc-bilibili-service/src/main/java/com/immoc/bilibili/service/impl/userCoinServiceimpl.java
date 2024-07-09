package com.immoc.bilibili.service.impl;

import com.immoc.bilibili.dao.UserCoinDao;
import com.immoc.bilibili.service.userCoinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;


@Service
public class userCoinServiceimpl implements userCoinService {
    @Autowired
    private UserCoinDao userCoinDao;

    @Override
    public Integer getUserCoinsAmount(Long userId) {

        return userCoinDao.getUserCoinsAmount(userId);
    }

    @Override
    public void updateUserCoinsAmount(Long userId, int amount) {
        Date updateTime = new Date();
        userCoinDao.updateUserCoinsAmount(userId,amount,updateTime);
    }
}
