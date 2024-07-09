package com.immoc.bilibili.service.impl;

import com.immoc.bilibili.dao.VideoDao;
import com.immoc.bilibili.domain.*;
import com.immoc.bilibili.domain.exception.ConditionException;
import com.immoc.bilibili.service.VideoService;
import com.immoc.bilibili.service.bilibiliService;
import com.immoc.bilibili.service.userCoinService;
import com.immoc.bilibili.service.util.FastDfsUtil;
import com.immoc.bilibili.service.util.IpUtil;
import eu.bitwalker.useragentutils.UserAgent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VideoServiceimpl implements VideoService {

    @Autowired
    private VideoDao videoDao;

    @Autowired
    private FastDfsUtil fastDfsUtil;

    @Autowired
    private userCoinService userCoinService;

    @Autowired
    private bilibiliService bilibiliService;

    @Override
    public void addVideos(Video video) {
        Date date = new Date();
        video.setCreateTime(date);
        videoDao.addVideos(video);
        Long videoId = video.getId();
        List<VideoTag> tagList = video.getVideoTagList();
        System.out.println(tagList);
        if(tagList!=null) {
            tagList.forEach(item -> {
                item.setCreateTime(date);
                item.setVideoId(videoId);
            });
            videoDao.batchAddVideoTags(tagList);
        }
    }

    @Override
    public PageResult<Video> pageListVideos(Integer size, Integer no, String area) {
        if(size==null || no==null){
            new ConditionException("参数异常!");
        }
        HashMap<String, Object> params = new HashMap<>();
        params.put("start",(no-1)*size);//起始第几条 比如一条5个数据 第一页第一个数据就是0 第二页第一个数据就是5
        params.put("limit",size);
        params.put("area",area);
        List<Video> list = new ArrayList<>();
        //根据视频所在分区查找 当前的分区视频个数
        Integer total = videoDao.pageCountVideos(params);
        if(total > 0){
            list = videoDao.pageListVideos(params);
        }
        return new PageResult<Video>(total,list);
    }

    @Override
    public void viewVideoOnlineBySlices(HttpServletRequest request, HttpServletResponse response, String url) throws Exception {
        fastDfsUtil.viewVideoOnlineBySlices(request,response,url);
    }

    @Override
    public void addVideoLike(Long videoId, Long userId) {
        Video video=videoDao.getVideoById(videoId);
        //如果当前视频不存在的话
        if(video==null){
            throw new ConditionException("视频不存在");
        }
        VideoLike videoLike =videoDao.getVideoLikeByVideoIdAndUserId(videoId,userId);
        //如果数据库显示当前 已经存在 说明该视频的user用户已经点赞过了
        if(videoLike!=null){
            throw new ConditionException("已经赞过");
        }
        videoLike = new VideoLike();
        videoLike.setVideoId(videoId);
        videoLike.setUserId(userId);
        videoLike.setCreateTime(new Date());
        videoDao.addVideoLike(videoLike);
    }

    @Override
    public void deleteVideoLike(Long videoId, Long userId) {
        videoDao.deleteVideoLike(videoId, userId);
    }

    @Override
    public Map<String, Object> getVideoLikes(Long videoId, Long userId) {
        //查询当前视频的点赞数量
        Long count=videoDao.getVideoLikes(videoId);
        //查询当前用户点在的视频信息
        VideoLike videoLike = videoDao.getVideoLikeByVideoIdAndUserId(videoId, userId);
        boolean like = videoLike != null;
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        result.put("like", like);
        return result;
    }

    @Override
    public void addVideoCollection(VideoCollection videoCollection, Long userId) {
        Long videoId = videoCollection.getVideoId();
        Long groupId = videoCollection.getGroupId();
        if(videoId == null || groupId == null){
            throw new ConditionException("参数异常！");
        }
        Video video = videoDao.getVideoById(videoId);
        if(video == null){
            throw new ConditionException("非法视频！");
        }
        /**
         * 先删除  在添加就是更新操作
         */
        //删除原有视频收藏
        videoDao.deleteVideoCollection(videoId, userId);
        //添加新的视频收藏
        videoCollection.setUserId(userId);
        videoCollection.setCreateTime(new Date());
        videoDao.addVideoCollection(videoCollection);
    }

    @Override
    public void deleteVideoCollection(Long videoId, Long userId) {
        videoDao.deleteVideoCollection(videoId,userId);
    }

    @Override
    public Map<String, Object> getVideoCollections(Long videoId, Long userId) {
        Long count = videoDao.getVideoCollections(videoId);
        VideoCollection videoCollection = videoDao.getVideoCollectionByVideoIdAndUserId(videoId, userId);
        boolean like = videoCollection != null;
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        result.put("like", like);
        return result;
    }

    @Override
    public void addVideoCoins(VideoCoin videoCoin, Long userId) {
        Long videoId = videoCoin.getVideoId();
        Integer amount = videoCoin.getAmount();
        if(videoId == null){
            throw new ConditionException("参数异常！");
        }
        //根据视频Id获取视频
        Video video = videoDao.getVideoById(videoId);
        if(video == null){
            throw new ConditionException("非法视频！");
        }
        //查询当前登录用户是否拥有足够的硬币
        Integer userCoinsAmount = userCoinService.getUserCoinsAmount(userId);
        //判断当前用户的硬币数量是否够投币的数量
        userCoinsAmount = userCoinsAmount == null ? 0 : userCoinsAmount;
        if(amount > userCoinsAmount){
            throw new ConditionException("硬币数量不足！");
        }
        userCoinsAmount = userCoinsAmount == null ? 0 : userCoinsAmount;
        if(amount > userCoinsAmount){
            throw new ConditionException("硬币数量不足！");
        }
        //查询当前登录用户对该视频已经投了多少硬币
        VideoCoin dbVideoCoin = videoDao.getVideoCoinByVideoIdAndUserId(videoId, userId);
        //新增视频投币
        if(dbVideoCoin == null){
            videoCoin.setUserId(userId);
            videoCoin.setCreateTime(new Date());
            videoDao.addVideoCoin(videoCoin);
        }else{
            Integer dbAmount = dbVideoCoin.getAmount();
            dbAmount += amount;
            //更新视频投币
            videoCoin.setUserId(userId);
            videoCoin.setAmount(dbAmount);
            videoCoin.setUpdateTime(new Date());
            videoDao.updateVideoCoin(videoCoin);
        }
        //更新用户当前硬币总数
        userCoinService.updateUserCoinsAmount(userId, (userCoinsAmount-amount));
    }

    @Override
    public Map<String, Object> getVideoCoins(Long videoId, Long userId) {
        //获取视频当前的投币数
        Long count = videoDao.getVideoCoinsAmount(videoId);
        //获取当前用户投币当前视频的所有详细信息
        VideoCoin videoCollection = videoDao.getVideoCoinByVideoIdAndUserId(videoId, userId);
        boolean like = videoCollection != null;
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        result.put("like", like);
        return result;
    }

    @Override
    public void addVideoComment(VideoComment videoComment, Long userId) {
        Long videoId = videoComment.getVideoId();
        if(videoId==null){
            throw  new ConditionException("参数异常");
        }
        Video video = videoDao.getVideoById(videoId);
        if(video==null){
            throw  new ConditionException("非法视频");
        }
        videoComment.setUserId(userId);
        videoComment.setCreateTime(new Date());
        videoDao.addVideoComment(videoComment);
    }

    @Override
    public PageResult<VideoComment> pageListVideoComments(Integer size, Integer no, Long videoId) {
        Video video = videoDao.getVideoById(videoId);
        if(video==null){
            throw new ConditionException("非法视频");
        }
        //将分页的参数保存起来
        Map<String, Object> params = new HashMap<>();
        params.put("start", (no-1)*size);
        params.put("limit", size);
        params.put("videoId", videoId);
        //查询出根评论的信息个数
        Integer total=videoDao.pageCountVideoComments(params);
        //存储评论详细信息
        List<VideoComment> list = new ArrayList<>();
        if(total>0){
            //查询根评论的详细信息 包括根评论下的二级评论
            list=videoDao.pageListVideoComments(params);
            //批量查询二级评论
            List<Long> parentIdList = list.stream().map(VideoComment::getId).collect(Collectors.toList());
            List<VideoComment> childCommentList =videoDao.batchGetVideoCommentsByRootIds(parentIdList);
            //获取当前评论的用户id,回应评论用户id 以及二级评论用户id
            Set<Long> userIdList = list.stream().map(VideoComment::getUserId).collect(Collectors.toSet());
            Set<Long> replyUserIdList = childCommentList.stream().map(VideoComment::getUserId).collect(Collectors.toSet());
            Set<Long> childUserIdList = childCommentList.stream().map(VideoComment::getReplyUserId).collect(Collectors.toSet());
            userIdList.addAll(replyUserIdList);
            userIdList.addAll(childUserIdList);
            //查询出所有用户id的详细信息
            List<UserInfo> userInfoList=bilibiliService.batchGetUserInfoByUserIds(userIdList);
            //将上述信息保存为Map（用户id:用户信息）
            Map<Long, UserInfo> userInfoMap = userInfoList.stream().collect(Collectors.toMap(UserInfo :: getUserId, userInfo -> userInfo));
            //遍历每一条评论 获取二级评论信息将其存储在List当中
            list.forEach(comment -> {
                Long id = comment.getId();
                List<VideoComment> childList = new ArrayList<>();
                childCommentList.forEach(child -> {
                    if(id.equals(child.getRootId())){
                        //获取二级评论的用户信息
                        child.setUserInfo(userInfoMap.get(child.getUserId()));
                        //获取回复用户id 的信息
                        child.setReplyUserInfo(userInfoMap.get(child.getReplyUserId()));
                        childList.add(child);
                    }
                });
                //存储二级评论信息
                comment.setChildList(childList);
                //存储一级评论用户信息
                comment.setUserInfo(userInfoMap.get(comment.getUserId()));
            });
        }
        return new PageResult<>(total, list);
    }

    @Override
    public Map<String, Object> getVideoDetails(Long videoId) {
        Video video = videoDao.getVideoById(videoId);
        Long userId = video.getUserId();
        //获取用户信息
        User user = bilibiliService.getUserInfo(userId);
        UserInfo userInfo = user.getUserInfo();
        Map<String, Object> result = new HashMap<>();
        result.put("video", video);
        result.put("userInfo", userInfo);
        return result;
    }

    @Override
    public void addVideoView(VideoView videoView, HttpServletRequest request) {
        //先获取当前视频的Id
        Long userId = videoView.getUserId();
        Long videoId = videoView.getVideoId();
        //生成clientId 请求头中的id
        String agent = request.getHeader("User-Agent");
        //获取userAagent
        UserAgent userAgent = UserAgent.parseUserAgentString(agent);
        String clientId = String.valueOf(userAgent.getId());
        String ip = IpUtil.getIP(request);
        HashMap<String, Object> param = new HashMap<>();
        //如果当前是用户登录模式就会有用户Id 否则就用Ip月ClientId进行判断游客
        if(userId!=null){
            param.put("userId",userId);
        }else {
            param.put("ip",ip);
            param.put("clientId", clientId);
        }
        //为了保证一天访问视频就算一个播放量
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        param.put("today", sdf.format(now));
        param.put("videoId", videoId);
        //判断是否存在当前的观看记录信息
        VideoView dbVideoView=videoDao.getvideoView(param);
        if(dbVideoView==null){
            videoView.setIp(ip);
            videoView.setClientId(clientId);
            videoView.setCreateTime(new Date());
            videoDao.addVideoView(videoView);
        }
    }

    @Override
    public int getVideoViewCounts(Long videoId) {
        return videoDao.getVideoViewCounts(videoId);
    }
}
