package com.hth.domain.activity.service.trial.node;

import com.hth.domain.activity.model.entity.MarketProductEntity;
import com.hth.domain.activity.model.entity.TrialBalanceEntity;
import com.hth.domain.activity.service.trial.AbstractGroupBuyMarketSupport;
import com.hth.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import com.hth.types.design.framework.tree.StrategyHandler;
import com.hth.types.enums.ResponseCode;
import com.hth.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 开关节点
 * @create 2024-12-14 14:27
 */
@Slf4j
@Service
public class SwitchRoot extends AbstractGroupBuyMarketSupport<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> {

    @Resource
    private MarketNode marketNode;

    @Override
    public TrialBalanceEntity doApply(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        String userId = requestParameter.getUserId();
        if(activityRepository.downgradeSwitch()){
            throw new AppException(ResponseCode.E0003.getCode(),ResponseCode.E0003.getInfo());
        }

        if(!activityRepository.cutRange(userId)){
            throw new AppException(ResponseCode.E0004.getCode(),ResponseCode.E0004.getInfo());
        }
        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> get(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        return marketNode;
    }
}
