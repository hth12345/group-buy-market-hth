package com.hth.domain.trade.service.refund.business;

import com.hth.domain.trade.model.entity.TradeRefundOrderEntity;
import com.hth.domain.trade.model.valobj.TeamRefundSuccess;

/**
 * 退单策略接口
 */
public interface IRefundOrderStrategy {
    void refundOrder(TradeRefundOrderEntity tradeRefundOrderEntity);

    void reverseStock(TeamRefundSuccess teamRefundSuccess);
}
