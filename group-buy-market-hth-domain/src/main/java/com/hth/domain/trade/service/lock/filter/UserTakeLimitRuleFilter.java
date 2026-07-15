package com.hth.domain.trade.service.lock.filter;

import com.hth.domain.trade.adapter.repository.ITradeRepository;
import com.hth.domain.trade.model.entity.GroupBuyActivityEntity;
import com.hth.domain.trade.model.entity.TradeLockRuleCommandEntity;
import com.hth.domain.trade.model.entity.TradeLockRuleFilterBackEntity;
import com.hth.domain.trade.service.lock.factory.TradeLockRuleFilterFactory;
import com.hth.types.design.framework.link.model2.handler.ILogicHandler;
import com.hth.types.enums.ResponseCode;
import com.hth.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
public class UserTakeLimitRuleFilter implements ILogicHandler<TradeLockRuleCommandEntity, TradeLockRuleFilterFactory.DynamicContext, TradeLockRuleFilterBackEntity> {
    @Resource
    private ITradeRepository repository;
    @Override
    public TradeLockRuleFilterBackEntity apply(TradeLockRuleCommandEntity requestParameter, TradeLockRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        long activityId = requestParameter.getActivityId();
        String userId = requestParameter.getUserId();

        GroupBuyActivityEntity groupBuyActivity = dynamicContext.getGroupBuyActivity();

        Integer takeLimitCount = groupBuyActivity.getTakeLimitCount();

        // 查询用户在一个拼团活动上参与的次数
        int count = repository.queryOrderCountByActivityId(activityId,userId);
        if(takeLimitCount != null && count >= takeLimitCount) {
            throw new AppException(ResponseCode.E0103.getCode(),ResponseCode.E0103.getInfo());
        }
        return TradeLockRuleFilterBackEntity.builder()
                .userTakeOrderCount(count)
                .build();
    }
}
