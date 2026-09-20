package com.hth.domain.trade.service.refund.business.impl;

import com.hth.domain.trade.model.entity.TradeRefundOrderEntity;
import com.hth.domain.trade.service.refund.business.IRefundOrderStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 已支付，未成团退单
 */
@Slf4j
@Service("paid2RefundStrategy")
public class Paid2RefundStrategy implements IRefundOrderStrategy {
    @Override
    public void refundOrder(TradeRefundOrderEntity tradeRefundOrderEntity) {

    }
}
