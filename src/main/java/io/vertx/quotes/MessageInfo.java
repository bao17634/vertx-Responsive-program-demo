package io.vertx.quotes;

import lombok.Data;

import java.util.List;

/**
 * @ClassName: MessageInfo
 * @Description: TODO
 * @Author: yanrong
 * @Date: 2019/12/4 18:50
 */
@Data
public class MessageInfo {
    private String errorCode;
    private String errorMsg;
    private List<Quote> data;
    @Override
    public String toString() {
        return "MessageInfo [errorCode=" + errorCode + ", errorMsg=" + errorMsg + ", data=" + data + "]";
    }
}
