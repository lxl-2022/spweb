package com.immoc.bilibili.repostitory;

import com.immoc.bilibili.domain.Video;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface VideoRepostiory extends ElasticsearchRepository<Video,Long> {

    //springDate会将名称分词 为find by title(标题) like（模糊查询）
    Video findByTitleLike(String keyword);

}
