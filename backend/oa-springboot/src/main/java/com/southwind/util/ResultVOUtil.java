package com.southwind.util;

import com.southwind.vo.ResultVO;

public class ResultVOUtil {
    public static ResultVO success(Object object){
        ResultVO resultVO = new ResultVO();
        resultVO.setCode(0);
        resultVO.setMessage("成功");
        resultVO.setData(object);
        return resultVO;
    }

    public static ResultVO fail(){
        ResultVO resultVO = new ResultVO();
        resultVO.setCode(-1);
        resultVO.setMessage("失败");
        return resultVO;
    }

    // 新增：带错误信息的 fail 方法
    public static ResultVO fail(String message){
        ResultVO resultVO = new ResultVO();
        resultVO.setCode(-1);
        resultVO.setMessage(message);
        return resultVO;
    }

    // 可选：带自定义错误码和错误信息的方法
    public static ResultVO fail(Integer code, String message){
        ResultVO resultVO = new ResultVO();
        resultVO.setCode(code);
        resultVO.setMessage(message);
        return resultVO;
    }
}