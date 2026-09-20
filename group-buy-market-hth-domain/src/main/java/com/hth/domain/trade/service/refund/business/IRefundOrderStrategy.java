package com.hth.domain.trade.service.refund.business;

import com.hth.domain.trade.model.entity.TradeRefundOrderEntity;

/**
 * 退单策略接口
 */
public interface IRefundOrderStrategy {
    void refundOrder(TradeRefundOrderEntity tradeRefundOrderEntity);
}
