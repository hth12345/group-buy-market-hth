package com.hth.domain.trade.service;

import com.hth.domain.trade.model.entity.TradePaySettlementEntity;
import com.hth.domain.trade.model.entity.TradePaySuccessEntity;

public interface ITradeSettlementOrderService {
    TradePaySettlementEntity settlementMarketPayOrder(TradePaySuccessEntity tradePaySuccessEntity) throws Exception;
}
