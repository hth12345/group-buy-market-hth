package com.hth.domain.trade.service.lock;

import com.hth.domain.trade.adapter.repository.ITradeRepository;
import com.hth.domain.trade.model.aggregate.GroupBuyOrderAggregate;
import com.hth.domain.trade.model.entity.*;
import com.hth.domain.trade.model.valobj.GroupBuyProgressVO;
import com.hth.domain.trade.service.ITradeLockOrderService;
import com.hth.domain.trade.service.lock.factory.TradeLockRuleFilterFactory;
import com.hth.types.design.framework.link.model2.chain.BusinessLinkedList;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
@Service
public class TradeLockOrderService implements ITradeLockOrderService {
    @Resource
    private ITradeRepository repository;
    @Resource
    private BusinessLinkedList<TradeLockRuleCommandEntity, TradeLockRuleFilterFactory.DynamicContext, TradeLockRuleFilterBackEntity> tradeRuleFilter;
    @Override
    public MarketPayOrderEntity queryNoPayMarketPayOrderByOutTradeNo(String userId, String outTradeNo) {
        return repository.queryNoPayMarketPayOrderByOutTradeNo(userId,outTradeNo);
    }

    @Override
    public GroupBuyProgressVO queryGroupBuyProgress(String teamId) {
        return repository.queryGroupBuyProgress(teamId);
    }

    //锁定，营销预支付订单；商品下单前，预购锁定。
    @Override
    public MarketPayOrderEntity lockMarketPayOrder(UserEntity userEntity, PayActivityEntity payActivityEntity, PayDiscountEntity payDiscountEntity) throws Exception {

        // 交易规则过滤
        TradeLockRuleFilterBackEntity tradeLockRuleFilterBackEntity = tradeRuleFilter.apply(TradeLockRuleCommandEntity.builder()
                .activityId(payActivityEntity.getActivityId())
                .userId(userEntity.getUserId())
                .build(), new TradeLockRuleFilterFactory.DynamicContext());

        // 已参与拼团量 - 用于构建数据库唯一索引使用，确保用户只能在一个活动上参与固定的次数
        Integer userTakeOrderCount = tradeLockRuleFilterBackEntity.getUserTakeOrderCount();

        // 查询拼团时长
        Integer validTime = repository.queryActivityValidTimeByActivityId(payActivityEntity.getActivityId());
        // 构建聚合对象
        GroupBuyOrderAggregate groupBuyOrderAggregate = GroupBuyOrderAggregate.builder()
                .payActivityEntity(payActivityEntity)
                .payDiscountEntity(payDiscountEntity)
                .userEntity(userEntity)
                .userTakeOrderCount(userTakeOrderCount)
                .validTime(validTime)
                .build();
        // 锁定聚合订单 - 这会用户只是下单还没有支付。后续会有2个流程；支付成功、超时未支付（回退）
        return repository.lockMarketPayOrder(groupBuyOrderAggregate);
    }
}
