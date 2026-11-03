package com.hth.domain.activity.service.filter;

import cn.bugstack.wrench.design.framework.link.model2.handler.ILogicHandler;
import com.hth.domain.activity.adapter.repository.IActivityRepository;
import com.hth.domain.activity.model.entity.ActivitiesBackEntity;
import com.hth.domain.activity.model.entity.ActivitiesCommandEntity;
import com.hth.domain.activity.service.factory.ActivitiesRuleFilterFactory;
import com.hth.domain.trade.adapter.repository.ITradeRepository;
import com.hth.types.enums.ResponseCode;
import com.hth.types.exception.AppException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
@Service
public class SCRulerFilter implements ILogicHandler<ActivitiesCommandEntity, ActivitiesRuleFilterFactory.DynamicContext, ActivitiesBackEntity> {
    @Resource
    private ITradeRepository repository;
    @Override
    public ActivitiesBackEntity apply(ActivitiesCommandEntity requestParameter, ActivitiesRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        boolean intercept = repository.isSCBlackIntercept(requestParameter.getSource(),requestParameter.getChannel());
        if (intercept) {
            throw new AppException(ResponseCode.E0105.getCode(),ResponseCode.E0105.getInfo());
        }
        return next(requestParameter, dynamicContext);
    }
}
