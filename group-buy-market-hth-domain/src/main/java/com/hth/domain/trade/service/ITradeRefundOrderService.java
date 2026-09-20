package com.hth.domain.trade.service;

import com.hth.domain.trade.model.entity.TradeRefundBehaviorEntity;
import com.hth.domain.trade.model.entity.TradeRefundCommandEntity;

/**
 * 退单，逆向流程接口
 */
public interface ITradeRefundOrderService {
    TradeRefundBehaviorEntity refundOrder(TradeRefundCommandEntity tradeRefundCommandEntity);
}
