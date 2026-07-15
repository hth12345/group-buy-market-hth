package com.hth.domain.trade.service.settlement.filter;

import com.hth.domain.trade.adapter.repository.ITradeRepository;
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
public class SCRuleFilter implements ILogicHandler<TradeSettlementRuleCommandEntity, TradeSettlementRuleFilterFactory.DynamicContext, TradeSettlementRuleFilterBackEntity> {
    @Resource
    private ITradeRepository repository;
    @Override
    public TradeSettlementRuleFilterBackEntity apply(TradeSettlementRuleCommandEntity requestParameter, TradeSettlementRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        boolean intercept = repository.isSCBlackIntercept(requestParameter.getSource(),requestParameter.getChannel());
        if (intercept) {
            throw new AppException(ResponseCode.E0105.getCode(),ResponseCode.E0105.getInfo());
        }
        return next(requestParameter, dynamicContext);
    }
}
