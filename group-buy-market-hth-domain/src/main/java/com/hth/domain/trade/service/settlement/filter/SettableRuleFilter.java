package com.hth.domain.trade.service.settlement.filter;

import com.hth.domain.trade.adapter.repository.ITradeRepository;
import com.hth.domain.trade.model.entity.GroupBuyTeamEntity;
import com.hth.domain.trade.model.entity.MarketPayOrderEntity;
import com.hth.domain.trade.model.entity.TradeSettlementRuleCommandEntity;
import com.hth.domain.trade.model.entity.TradeSettlementRuleFilterBackEntity;
import com.hth.domain.trade.service.settlement.factory.TradeSettlementRuleFilterFactory;
import com.hth.types.design.framework.link.model2.handler.ILogicHandler;
import com.hth.types.enums.ResponseCode;
import com.hth.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
public class SettableRuleFilter implements ILogicHandler<TradeSettlementRuleCommandEntity, TradeSettlementRuleFilterFactory.DynamicContext, TradeSettlementRuleFilterBackEntity> {
    @Resource
    private ITradeRepository repository;
    @Override
    public TradeSettlementRuleFilterBackEntity apply(TradeSettlementRuleCommandEntity requestParameter, TradeSettlementRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        MarketPayOrderEntity marketPayOrderEntity = dynamicContext.getMarketPayOrderEntity();
        GroupBuyTeamEntity groupBuyTeamEntity =  repository.queryGroupBuyTeamByTeamId(marketPayOrderEntity.getTeamId());
        if (null == groupBuyTeamEntity) {
            throw new AppException(ResponseCode.E0107.getCode(), ResponseCode.E0107.getInfo());
        }
        if(requestParameter.getOutTradeTime().after(groupBuyTeamEntity.getValidEndTime())){
            throw new AppException(ResponseCode.E0106.getCode(),ResponseCode.E0106.getInfo());
        }
        dynamicContext.setGroupBuyTeamEntity(groupBuyTeamEntity);
        return next(requestParameter, dynamicContext);
    }
}
