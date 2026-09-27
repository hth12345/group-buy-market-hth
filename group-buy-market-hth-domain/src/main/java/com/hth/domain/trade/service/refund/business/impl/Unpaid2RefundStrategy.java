package com.hth.domain.trade.service.refund.business.impl;

import com.alibaba.fastjson.JSON;
import com.hth.domain.trade.adapter.repository.ITradeRepository;
import com.hth.domain.trade.model.aggregate.GroupBuyRefundAggregate;
import com.hth.domain.trade.model.entity.NotifyTaskEntity;
import com.hth.domain.trade.model.entity.TradeRefundOrderEntity;
import com.hth.domain.trade.model.valobj.TeamRefundSuccess;
import com.hth.domain.trade.service.ITradeTaskService;
import com.hth.domain.trade.service.lock.factory.TradeLockRuleFilterFactory;
import com.hth.domain.trade.service.refund.business.AbstractRefundOrderStrategy;
import com.hth.domain.trade.service.refund.business.IRefundOrderStrategy;
import com.hth.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 未支付，未成团退单
 */
@Slf4j
@Service("unpaid2RefundStrategy")
public class Unpaid2RefundStrategy extends AbstractRefundOrderStrategy implements IRefundOrderStrategy {
    @Resource
    private ITradeRepository repository;
    @Override
    public void refundOrder(TradeRefundOrderEntity tradeRefundOrderEntity) {
        log.info("退单；未支付，未成团 userId:{} teamId:{} orderId:{}", tradeRefundOrderEntity.getUserId(), tradeRefundOrderEntity.getTeamId(), tradeRefundOrderEntity.getOrderId());
        // 1. 退单；未支付，未成团
        NotifyTaskEntity notifyTaskEntity = repository.unpaid2Refund(GroupBuyRefundAggregate.buildUnpaid2RefundAggregate(tradeRefundOrderEntity, -1));

        // 2. 发送MQ消息 - 发送MQ，恢复锁单库存量使用
        sendRefundNotifyMessage(notifyTaskEntity, "未支付，未成团");
    }

    @Override
    public void reverseStock(TeamRefundSuccess teamRefundSuccess) throws Exception {
        doReverseStock(teamRefundSuccess, "未支付，未成团，但有锁单记录，要恢复锁单库存");
    }
}

