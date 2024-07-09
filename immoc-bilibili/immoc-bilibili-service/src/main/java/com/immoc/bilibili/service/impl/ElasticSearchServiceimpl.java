package com.immoc.bilibili.service.impl;

import com.immoc.bilibili.domain.UserInfo;
import com.immoc.bilibili.domain.Video;
import com.immoc.bilibili.repostitory.UserInfoRepostiory;
import com.immoc.bilibili.repostitory.VideoRepostiory;
import com.immoc.bilibili.service.ElasticSearchService;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@Service
public class ElasticSearchServiceimpl implements ElasticSearchService {

    @Autowired
    private VideoRepostiory videoRepostiory;

    @Autowired
    private UserInfoRepostiory userInfoRepostiory;

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    //将数据添加到es当中
    @Override
    public void addVideo(Video video) {
        videoRepostiory.save(video);
    }

    //根据视频Title进行查询
    @Override
    public Video getVideos(String keyword) {
        Video video = videoRepostiory.findByTitleLike(keyword);
        return video;
    }

    //将用户信息添加进去 满足全文搜索中也可以搜寻用户的信息
    @Override
    public void addUserInfo(UserInfo userInfo) {
        userInfoRepostiory.save(userInfo);
    }

    //进行全文搜索
    public List<Map<String, Object>> getContents(String keyword,
                                                 Integer pageNo,
                                                 Integer pageSize) throws IOException {
        //拿出要查询的索引库
        String[] indices = {"videos", "user-infos"};

        //将查询的请求信息用searchRequest包装
        SearchRequest searchRequest = new SearchRequest(indices);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        //分页
        sourceBuilder.from(pageNo - 1);
        sourceBuilder.size(pageSize);
        MultiMatchQueryBuilder matchQueryBuilder = QueryBuilders.multiMatchQuery(keyword, "title", "nick", "description");
        sourceBuilder.query(matchQueryBuilder);
        searchRequest.source(sourceBuilder);


        //设置查询过期时间
        sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
        //高亮显示 的Field字段名称
        String[] array = {"title", "nick", "description"};
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        for(String key : array){
            highlightBuilder.fields().add(new HighlightBuilder.Field(key));
        }
        highlightBuilder.requireFieldMatch(false); //如果要多个字段进行高亮，要为false
        highlightBuilder.preTags("<span style=\"color:red\">");
        highlightBuilder.postTags("</span>");
        sourceBuilder.highlighter(highlightBuilder);

        //执行搜索
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        List<Map<String, Object>> arrayList = new ArrayList<>();
        //拿到所有命中的信息
        for(SearchHit hit : searchResponse.getHits()){
            //处理高亮字段
            Map<String, HighlightField> highLightBuilderFields = hit.getHighlightFields();
            //取_source字段值 就是查询出来的数据
            Map<String, Object> sourceMap = hit.getSourceAsMap();
            for(String key : array){
                HighlightField field = highLightBuilderFields.get(key);
                if(field != null){
                    //将高亮字段命中的信息获取出来并且转换成字符串形式用sourceMap存储
                    Text[] fragments = field.fragments();
                    String str = Arrays.toString(fragments);
                    str = str.substring(1, str.length()-1);
                    //对数据重新封装 转换成str
                    sourceMap.put(key, str);
                }
            }
            arrayList.add(sourceMap);
        }
        return arrayList;
    }


}
