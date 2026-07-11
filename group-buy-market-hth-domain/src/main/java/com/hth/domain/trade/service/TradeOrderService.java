package com.hth.domain.trade.service;

import com.hth.domain.trade.adapter.repository.ITradeRepository;
import com.hth.domain.trade.model.aggregate.GroupBuyOrderAggregate;
import com.hth.domain.trade.model.entity.MarketPayOrderEntity;
import com.hth.domain.trade.model.entity.PayActivityEntity;
import com.hth.domain.trade.model.entity.PayDiscountEntity;
import com.hth.domain.trade.model.entity.UserEntity;
import com.hth.domain.trade.model.valobj.GroupBuyProgressVO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
@Service
public class TradeOrderService implements ITradeOrderService {
    @Resource
    private ITradeRepository repository;
    @Override
    public MarketPayOrderEntity queryNoPayMarketPayOrderByOutTradeNo(String userId, String outTradeNo) {
        return repository.queryNoPayMarketPayOrderByOutTradeNo(userId,outTradeNo);
    }

    @Override
    public GroupBuyProgressVO queryGroupBuyProgress(String teamId) {
        return repository.queryGroupBuyProgress(teamId);
    }

    @Override
    public MarketPayOrderEntity lockMarketPayOrder(UserEntity userEntity, PayActivityEntity payActivityEntity, PayDiscountEntity payDiscountEntity) {
        GroupBuyOrderAggregate groupBuyOrderAggregate = GroupBuyOrderAggregate.builder()
                .payActivityEntity(payActivityEntity)
                .payDiscountEntity(payDiscountEntity)
                .userEntity(userEntity)
                .build();
        return repository.lockMarketPayOrder(groupBuyOrderAggregate);
    }
}
