package com.hth.domain.trade.service.refund.business.impl;

import com.hth.domain.trade.adapter.repository.ITradeRepository;
import com.hth.domain.trade.model.aggregate.GroupBuyRefundAggregate;
import com.hth.domain.trade.model.entity.TradeRefundOrderEntity;
import com.hth.domain.trade.service.refund.business.IRefundOrderStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 未支付，未成团退单
 */
@Slf4j
@Service("unpaid2RefundStrategy")
public class Unpaid2RefundStrategy implements IRefundOrderStrategy {
    @Resource
    private ITradeRepository repository;
    @Override
    public void refundOrder(TradeRefundOrderEntity tradeRefundOrderEntity) {
        repository.unpaid2Refund(GroupBuyRefundAggregate.buildUnpaid2RefundAggregate(tradeRefundOrderEntity, -1));
    }
}
