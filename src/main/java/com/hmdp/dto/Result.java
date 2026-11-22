package com.hmdp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result {
    private Boolean success;
    private String errorMsg;
    private Object data;
    private Long total;

    /**
     * 成功
     * @return
     */
    public static Result ok(){
        return new Result(true, null, null, null);
    }

    /**
     * 成功
     * @param data
     * @return
     */
    public static Result ok(Object data){

        return new Result(true, null, data, null);
    }

    /**
     * 成功
     * @param data
     * @param total
     * @return
     */
    public static Result ok(List<?> data, Long total){

        return new Result(true, null, data, total);
    }

    /**
     * 失败
     * @param errorMsg
     * @return
     */
    public static Result fail(String errorMsg){
        return new Result(false, errorMsg, null, null);
    }
}
