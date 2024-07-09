package com.immoc.bilibili.service.Hadler;


import com.immoc.bilibili.domain.JsonResponse;
import com.immoc.bilibili.domain.exception.ConditionException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

//@order声明最高的优先级
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CommonGloblelException {

    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    public JsonResponse<String> commonExceptionHandler(HttpServletRequest request,Exception e){
        String errMsg=e.getMessage();
        if(e instanceof ConditionException){
            String code=((ConditionException)e).getCode();
            return new JsonResponse<>(code,errMsg);
        }else {
            return new JsonResponse<>(errMsg);
        }
    }
}
