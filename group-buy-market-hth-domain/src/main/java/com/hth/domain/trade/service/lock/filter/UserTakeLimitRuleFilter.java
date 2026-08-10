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
        log.info("交易规则过滤-用户参与次数校验{} activityId:{}", requestParameter.getUserId(), requestParameter.getActivityId());

        long activityId = requestParameter.getActivityId();
        String userId = requestParameter.getUserId();

        GroupBuyActivityEntity groupBuyActivity = dynamicContext.getGroupBuyActivity();

        Integer takeLimitCount = groupBuyActivity.getTakeLimitCount();

        // 查询用户在一个拼团活动上参与的次数
        int count = repository.queryOrderCountByActivityId(activityId,userId);
        if(takeLimitCount != null && count >= takeLimitCount) {
            log.info("用户参与次数校验，已达可参与上限 activityId:{}", requestParameter.getActivityId());
            throw new AppException(ResponseCode.E0103.getCode(),ResponseCode.E0103.getInfo());
        }
        dynamicContext.setUserTakeOrderCount(count);
        // 走到下一个责任链
        return next(requestParameter,dynamicContext);
    }
}
