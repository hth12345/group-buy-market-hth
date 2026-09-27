package com.hth.domain.trade.service;

import com.hth.domain.activity.model.entity.UserGroupBuyOrderDetailEntity;
import com.hth.domain.trade.model.entity.TradeRefundBehaviorEntity;
import com.hth.domain.trade.model.entity.TradeRefundCommandEntity;
import com.hth.domain.trade.model.valobj.TeamRefundSuccess;

import java.util.List;

/**
 * 退单，逆向流程接口
 */
public interface ITradeRefundOrderService {
    TradeRefundBehaviorEntity refundOrder(TradeRefundCommandEntity tradeRefundCommandEntity) throws Exception;

    void restoreTeamLockStock(TeamRefundSuccess teamRefundSuccess) throws Exception;

    List<UserGroupBuyOrderDetailEntity> queryTimeoutUnpaidOrderList();

}
