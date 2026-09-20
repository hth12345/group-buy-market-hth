package com.hth.api.dto;

import lombok.Data;
@Data
public class LockMarketPayOrderRequestDTO {

    // 用户ID
    private String userId;
    // 拼单组队ID - 可为空，为空则创建新组队ID
    private String teamId;
    // 活动ID
    private Long activityId;
    // 商品ID
    private String goodsId;
    // 渠道
    private String source;
    // 来源
    private String channel;
    // 外部交易单号
    private String outTradeNo;
    // 回调配置
    private NotifyConfigVO notifyConfigVO;

    // MQ的set方法
    public void setNotifyMQ(){
        NotifyConfigVO notifyConfigVO = new NotifyConfigVO();
        notifyConfigVO.setNotifyType("MQ");
        this.notifyConfigVO = notifyConfigVO;
    }
    // HTTP的set方法
    public void setNotifyUrl(String notifyUrl){
        NotifyConfigVO notifyConfigVO = new NotifyConfigVO();
        notifyConfigVO.setNotifyType("HTTP");
        notifyConfigVO.setNotifyUrl(notifyUrl);
        this.notifyConfigVO = notifyConfigVO;
    }

    @Data
    public static class NotifyConfigVO {
        private String notifyType;
        private String notifyMQ;
        private String notifyUrl;
    }
}
