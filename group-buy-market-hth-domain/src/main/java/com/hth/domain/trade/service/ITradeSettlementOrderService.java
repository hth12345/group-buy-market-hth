package com.hth.domain.trade.service;

import com.hth.domain.trade.model.entity.TradePaySettlementEntity;
import com.hth.domain.trade.model.entity.TradePaySuccessEntity;

import java.util.Map;

public interface ITradeSettlementOrderService {
    //支付结算
    TradePaySettlementEntity settlementMarketPayOrder(TradePaySuccessEntity tradePaySuccessEntity) throws Exception;
}
