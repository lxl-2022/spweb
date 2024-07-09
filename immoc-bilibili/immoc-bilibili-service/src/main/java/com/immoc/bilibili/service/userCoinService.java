package com.immoc.bilibili.service;

public interface userCoinService {
    Integer getUserCoinsAmount(Long userId);

    void updateUserCoinsAmount(Long userId, int i);
}
