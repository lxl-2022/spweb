package com.immoc.bilibili.domain.exception;

public class ConditionException extends RuntimeException{
    //序列化使用的
    private static final long serialversionUID=1L;

    String code;

    public ConditionException(String code,String name){
        super(name);
        this.code=code;
    }

    public ConditionException(String name){
        super(name);
        code="500";
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
