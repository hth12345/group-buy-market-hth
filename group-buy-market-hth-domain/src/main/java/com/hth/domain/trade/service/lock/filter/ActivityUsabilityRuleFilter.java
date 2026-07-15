package com.hth.domain.trade.service.lock.filter;

import com.hth.domain.trade.adapter.repository.ITradeRepository;
import com.hth.domain.trade.model.entity.GroupBuyActivityEntity;
import com.hth.domain.trade.model.entity.TradeLockRuleCommandEntity;
import com.hth.domain.trade.model.entity.TradeLockRuleFilterBackEntity;
import com.hth.domain.trade.service.lock.factory.TradeLockRuleFilterFactory;
import com.hth.types.design.framework.link.model2.handler.ILogicHandler;
import com.hth.types.enums.ActivityStatusEnumVO;
import com.hth.types.enums.ResponseCode;
import com.hth.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

@Slf4j
@Service
public class ActivityUsabilityRuleFilter implements ILogicHandler<TradeLockRuleCommandEntity, TradeLockRuleFilterFactory.DynamicContext, TradeLockRuleFilterBackEntity> {

    @Resource
    private ITradeRepository repository;
    @Override
    public TradeLockRuleFilterBackEntity apply(TradeLockRuleCommandEntity requestParameter, TradeLockRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        long activityId = requestParameter.getActivityId();
        //查询拼团活动
        GroupBuyActivityEntity groupBuyActivityEntity = repository.queryGroupBuyActivityByActivityId(activityId);

        // 校验；活动存在
        if(groupBuyActivityEntity == null){
            throw new AppException(ResponseCode.E0009.getCode(),ResponseCode.E0009.getInfo());
        }

        // 校验；活动状态 - 可以抛业务异常code，或者把code写入到动态上下文dynamicContext中，最后获取。
        if(!groupBuyActivityEntity.getStatus().equals(ActivityStatusEnumVO.EFFECTIVE)){
            throw new AppException(ResponseCode.E0101.getCode(),ResponseCode.E0101.getInfo());
        }

        // 校验；活动时间
        Date currentDate = new Date();
        if(currentDate.before(groupBuyActivityEntity.getStartTime())||currentDate.after(groupBuyActivityEntity.getEndTime())){
            throw new AppException(ResponseCode.E0102.getCode(),ResponseCode.E0102.getInfo());
        }
        // 写入动态上下文
        dynamicContext.setGroupBuyActivity(groupBuyActivityEntity);

        // 走到下一个责任链节点
        return next(requestParameter, dynamicContext);
    }

}
