package com.immoc.bilibili.domain;

public class JsonResponse<T> {

    private String code;
    private  String message;
    private  T data;

    public JsonResponse(String code,String message){
        this.code=code;
        this.message=message;
    }

    public JsonResponse(T data) {
        this.data = data;
        message="成功";
        code="0";
    }

    //应用场景时一些不需要给前端返回参数的返回方法
    public static  JsonResponse<String> success(){
        return new JsonResponse<>(null);
    }

    //后台需要给前端传递数据 比如登录传递令牌信息
    public static JsonResponse<String> success(String data){
        return new JsonResponse<>(data);
    }

    public static  JsonResponse<String> fail(){
        return new JsonResponse<>("1","失败");
    }

    public static  JsonResponse<String> fail(String code,String msg){
        return new JsonResponse<>(code,msg);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
