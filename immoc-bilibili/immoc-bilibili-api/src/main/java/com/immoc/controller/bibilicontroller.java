package com.immoc.controller;

import com.alibaba.fastjson.JSONObject;
import com.immoc.bilibili.domain.JsonResponse;
import com.immoc.bilibili.domain.PageResult;
import com.immoc.bilibili.domain.User;
import com.immoc.bilibili.domain.UserInfo;
import com.immoc.bilibili.service.UserServiceFollowingService;
import com.immoc.bilibili.service.bilibiliService;
import com.immoc.bilibili.service.util.RSAUtil;
import com.immoc.support.UserSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
public class bibilicontroller {

    @Autowired
    private bilibiliService userService;

    @Autowired
    private UserSupport userSupport;

    @Autowired
    private UserServiceFollowingService userFollowingService;

    //从token中获取userid 查询userinfo
    @GetMapping("/users")
    public JsonResponse<User> getUserInfo(){
        Long id = userSupport.getCurrentUserId();
        User user=userService.getUserInfo(id);
        return new JsonResponse<>(user);
    }

    @GetMapping("/rsa-pks")
    public JsonResponse<String> getRsaKey(){
        String pk = RSAUtil.getPublicKeyStr();
        return new JsonResponse<>(pk);
    }

    //注册
    @PostMapping("/users")
    public JsonResponse<String> adduser(@RequestBody User user){
        userService.adduser(user);
        return  JsonResponse.success();
    }

    //获取token值
    @PostMapping("/user-tokens")
    public JsonResponse<String> login(@RequestBody User user){
        String token=userService.login(user);
        return new JsonResponse<String>(token);
    }

    //修改UserInfor的信息
    @PostMapping("/user-infos")
    public JsonResponse<String> updateUserInfos(@RequestBody UserInfo userInfo){
        //从token中获取当前的id 可以防止有人恶意篡改信息
        Long userId = userSupport.getCurrentUserId();
        userInfo.setUserId(userId);
        userService.updateUserInfos(userInfo);
        return JsonResponse.success();
    }

    //根据用户传入的参数进行分页查询
    @GetMapping("/user-infos")
    public JsonResponse<PageResult<UserInfo>> pageListUserInfos(@RequestParam Integer no, @RequestParam Integer size, String nick){
        Long userId = userSupport.getCurrentUserId();
        JSONObject params = new JSONObject();
        params.put("no", no);
        params.put("size", size);
        params.put("nick", nick);
        params.put("userId", userId);
        //按照分页将粉丝关注列表获取
        PageResult<UserInfo> result = userService.pageListUserInfos(params);
        if(result.getTotal() > 0){
            //判断是否互相关注 互相关注的都设置了
            List<UserInfo> checkedUserInfoList = userFollowingService.checkFollowingStatus(result.getList(), userId);
            result.setList(checkedUserInfoList);
        }
        return new JsonResponse<>(result);
    }

    //用户登录时候 返回两个token 一个是aceesToken前端可以获取到 一个是refreshToken存储在数据库或者redis中无法获取 每当aceesToken失效以后去验证
    //refreshToken
    @PostMapping("/user-dts")
    public  JsonResponse<Map<String,Object>> loginForDts(@RequestBody User user) throws Exception {
        Map<String, Object> map = userService.loginForDts(user);
        return new JsonResponse<>(map);
    }

    //删除refreshToken
    @DeleteMapping("/refresh-tokens")
    public JsonResponse<String> logout(HttpServletRequest request){
        String refreshToken = request.getHeader("refreshToken");
        Long userId = userSupport.getCurrentUserId();
        userService.logout(refreshToken, userId);
        return JsonResponse.success();
    }

    //当access-tokens过期以后去判断 refresh-token是否过期 如果没有过期生成新的access与refreshtoken
    @PostMapping("/access-tokens")
    public JsonResponse<String> refreshAccessToken(HttpServletRequest request) throws Exception {
        String refreshToken = request.getHeader("refreshToken");
        String accessToken = userService.refreshAccessToken(refreshToken);
        return new JsonResponse<>(accessToken);
    }

}
