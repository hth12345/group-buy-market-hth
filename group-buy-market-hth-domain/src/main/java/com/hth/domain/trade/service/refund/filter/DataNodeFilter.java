package com.hth.domain.trade.service.refund.filter;

import cn.bugstack.wrench.design.framework.link.model2.handler.ILogicHandler;
import com.hth.domain.trade.adapter.repository.ITradeRepository;
import com.hth.domain.trade.model.entity.GroupBuyTeamEntity;
import com.hth.domain.trade.model.entity.MarketPayOrderEntity;
import com.hth.domain.trade.model.entity.TradeRefundBehaviorEntity;
import com.hth.domain.trade.model.entity.TradeRefundCommandEntity;
import com.hth.domain.trade.model.valobj.TradeOrderStatusEnumVO;
import com.hth.domain.trade.service.refund.factory.TradeRefundRuleFilterFactory;
import com.hth.types.enums.GroupBuyOrderEnumVO;
import com.hth.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@Slf4j
public class DataNodeFilter implements ILogicHandler<TradeRefundCommandEntity, TradeRefundRuleFilterFactory.DynamicContext, TradeRefundBehaviorEntity> {
    @Resource
    private ITradeRepository repository;
    @Override
    public TradeRefundBehaviorEntity apply(TradeRefundCommandEntity tradeRefundCommandEntity, TradeRefundRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        log.info("逆向流程-退单操作，数据加载节点 userId:{} outTradeNo:{}", tradeRefundCommandEntity.getUserId(), tradeRefundCommandEntity.getOutTradeNo());

        // 1. 查询外部交易单，组队id、orderId、拼团状态
        MarketPayOrderEntity marketPayOrderEntity = repository.queryMarketPayOrderEntityByOutTradeNo(tradeRefundCommandEntity.getUserId(), tradeRefundCommandEntity.getOutTradeNo());
        String teamId = marketPayOrderEntity.getTeamId();

        // 2. 查询拼团状态
        GroupBuyTeamEntity groupBuyTeamEntity = repository.queryGroupBuyTeamByTeamId(teamId);

        // 3. 写入上下文；如果查询数据是比较多的，可以参考 MarketNode2CompletableFuture 通过多线程进行加载
        dynamicContext.setMarketPayOrderEntity(marketPayOrderEntity);
        dynamicContext.setGroupBuyTeamEntity(groupBuyTeamEntity);

        return next(tradeRefundCommandEntity, dynamicContext);
    }
}
