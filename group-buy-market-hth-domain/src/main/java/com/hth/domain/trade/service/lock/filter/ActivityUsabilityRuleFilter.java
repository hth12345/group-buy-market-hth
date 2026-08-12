package com.hth.domain.trade.service.lock.filter;

import cn.bugstack.wrench.design.framework.link.model2.handler.ILogicHandler;
import com.hth.domain.trade.adapter.repository.ITradeRepository;
import com.hth.domain.trade.model.entity.GroupBuyActivityEntity;
import com.hth.domain.trade.model.entity.TradeLockRuleCommandEntity;
import com.hth.domain.trade.model.entity.TradeLockRuleFilterBackEntity;
import com.hth.domain.trade.service.lock.factory.TradeLockRuleFilterFactory;
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
        log.info("交易规则过滤-活动的可用性校验{} activityId:{}", requestParameter.getUserId(), requestParameter.getActivityId());

        long activityId = requestParameter.getActivityId();
        //查询拼团活动
        GroupBuyActivityEntity groupBuyActivityEntity = repository.queryGroupBuyActivityByActivityId(activityId);

        // 校验；活动存在
        if(groupBuyActivityEntity == null){
            log.info("活动的可用性校验，非存在该活动 activityId:{}", requestParameter.getActivityId());
            throw new AppException(ResponseCode.E0009.getCode(),ResponseCode.E0009.getInfo());
        }

        // 校验；活动状态 - 可以抛业务异常code，或者把code写入到动态上下文dynamicContext中，最后获取。
        if(!groupBuyActivityEntity.getStatus().equals(ActivityStatusEnumVO.EFFECTIVE)){
            log.info("活动的可用性校验，非生效状态 activityId:{}", requestParameter.getActivityId());
            throw new AppException(ResponseCode.E0101.getCode(),ResponseCode.E0101.getInfo());
        }

        // 校验；活动时间
        Date currentDate = new Date();
        if(currentDate.before(groupBuyActivityEntity.getStartTime())||currentDate.after(groupBuyActivityEntity.getEndTime())){
            log.info("活动的可用性校验，非可参与时间范围 activityId:{}", requestParameter.getActivityId());
            throw new AppException(ResponseCode.E0102.getCode(),ResponseCode.E0102.getInfo());
        }
        // 写入动态上下文
        dynamicContext.setGroupBuyActivity(groupBuyActivityEntity);

        // 走到下一个责任链节点
        return next(requestParameter, dynamicContext);
    }

}
