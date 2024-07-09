package com.immoc.bilibili.repostitory;

import com.immoc.bilibili.domain.UserInfo;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface UserInfoRepostiory extends ElasticsearchRepository<UserInfo,Long> {
}
