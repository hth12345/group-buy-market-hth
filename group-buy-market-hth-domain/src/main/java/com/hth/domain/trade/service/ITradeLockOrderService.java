package com.hth.domain.trade.service;

import com.hth.domain.trade.model.entity.MarketPayOrderEntity;
import com.hth.domain.trade.model.entity.PayActivityEntity;
import com.hth.domain.trade.model.entity.PayDiscountEntity;
import com.hth.domain.trade.model.entity.UserEntity;
import com.hth.domain.trade.model.valobj.GroupBuyProgressVO;

public interface ITradeLockOrderService {
    //查询，未被支付消费完成的营销优惠订单
    MarketPayOrderEntity queryNoPayMarketPayOrderByOutTradeNo(String userId, String outTradeNo);
    //查询拼团进度
    GroupBuyProgressVO queryGroupBuyProgress(String teamId);
    //锁定，营销预支付订单；商品下单前，预购锁定。
    MarketPayOrderEntity lockMarketPayOrder (UserEntity userEntity, PayActivityEntity payActivityEntity, PayDiscountEntity payDiscountEntity) throws Exception;

}
