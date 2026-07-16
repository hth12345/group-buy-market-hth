package com.hth.domain.trade.service.settlement.filter;

import com.hth.domain.trade.model.entity.GroupBuyTeamEntity;
import com.hth.domain.trade.model.entity.MarketPayOrderEntity;
import com.hth.domain.trade.model.entity.TradeSettlementRuleCommandEntity;
import com.hth.domain.trade.model.entity.TradeSettlementRuleFilterBackEntity;
import com.hth.domain.trade.service.settlement.factory.TradeSettlementRuleFilterFactory;
import com.hth.types.design.framework.link.model2.handler.ILogicHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EndRuleFilter implements ILogicHandler<TradeSettlementRuleCommandEntity, TradeSettlementRuleFilterFactory.DynamicContext, TradeSettlementRuleFilterBackEntity> {
    @Override
    public TradeSettlementRuleFilterBackEntity apply(TradeSettlementRuleCommandEntity requestParameter, TradeSettlementRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        GroupBuyTeamEntity groupBuyTeamEntity = dynamicContext.getGroupBuyTeamEntity();
       return TradeSettlementRuleFilterBackEntity.builder()
              .teamId(groupBuyTeamEntity.getTeamId())
              .activityId(groupBuyTeamEntity.getActivityId())
              .targetCount(groupBuyTeamEntity.getTargetCount())
              .completeCount(groupBuyTeamEntity.getCompleteCount())
              .lockCount(groupBuyTeamEntity.getLockCount())
              .status(groupBuyTeamEntity.getStatus())
              .validStartTime(groupBuyTeamEntity.getValidStartTime())
              .validEndTime(groupBuyTeamEntity.getValidEndTime())
               .notifyUrl(groupBuyTeamEntity.getNotifyUrl())
              .build();
    }
}
