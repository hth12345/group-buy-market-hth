package com.hth.domain.trade.service;

import com.hth.domain.trade.model.entity.TradePaySettlementEntity;
import com.hth.domain.trade.model.entity.TradePaySuccessEntity;

import java.util.Map;

public interface ITradeSettlementOrderService {
    //支付结算
    TradePaySettlementEntity settlementMarketPayOrder(TradePaySuccessEntity tradePaySuccessEntity) throws Exception;

    //通过定时执行回调任务
    Map<String,Integer> execSettlementNotifyJob() throws Exception;

    //完成拼团后自动执行回调任务
    Map<String, Integer> execSettlementNotifyJob(String teamId) throws Exception;
}
