package com.hth.domain.trade.service.filter;

import com.hth.domain.trade.adapter.repository.ITradeRepository;
import com.hth.domain.trade.model.entity.GroupBuyActivityEntity;
import com.hth.domain.trade.model.entity.TradeRuleCommandEntity;
import com.hth.domain.trade.model.entity.TradeRuleFilterBackEntity;
import com.hth.domain.trade.service.factory.TradeRuleFilterFactory;
import com.hth.types.design.framework.link.model2.handler.ILogicHandler;
import com.hth.types.enums.ResponseCode;
import com.hth.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
public class UserTakeLimitRuleFilter implements ILogicHandler<TradeRuleCommandEntity, TradeRuleFilterFactory.DynamicContext, TradeRuleFilterBackEntity> {
    @Resource
    private ITradeRepository repository;
    @Override
    public TradeRuleFilterBackEntity apply(TradeRuleCommandEntity requestParameter, TradeRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        long activityId = requestParameter.getActivityId();
        String userId = requestParameter.getUserId();

        GroupBuyActivityEntity groupBuyActivity = dynamicContext.getGroupBuyActivity();

        Integer takeLimitCount = groupBuyActivity.getTakeLimitCount();

        // 查询用户在一个拼团活动上参与的次数
        int count = repository.queryOrderCountByActivityId(activityId,userId);
        if(takeLimitCount != null && count >= takeLimitCount) {
            throw new AppException(ResponseCode.E0103.getCode(),ResponseCode.E0103.getInfo());
        }
        return TradeRuleFilterBackEntity.builder()
                .userTakeOrderCount(count)
                .build();
    }
}
