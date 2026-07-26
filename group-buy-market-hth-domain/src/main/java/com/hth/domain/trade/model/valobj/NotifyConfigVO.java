package com.hth.domain.trade.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.units.qual.A;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotifyConfigVO {
    //回调类型
    private NotifyTypeEnumVO notifyType;
    //回调地址
    private String notifyUrl;
    //回调消息
    private String notifyMQ;
}
