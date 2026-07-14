package com.hth.domain.trade.adapter.repository;

import com.hth.domain.trade.model.aggregate.GroupBuyOrderAggregate;
import com.hth.domain.trade.model.aggregate.GroupBuyTeamSettlementAggregate;
import com.hth.domain.trade.model.entity.GroupBuyActivityEntity;
import com.hth.domain.trade.model.entity.GroupBuyTeamEntity;
import com.hth.domain.trade.model.entity.MarketPayOrderEntity;
import com.hth.domain.trade.model.valobj.GroupBuyProgressVO;

public interface ITradeRepository {
    MarketPayOrderEntity queryNoPayMarketPayOrderByOutTradeNo(String userId, String outTradeNo);

    GroupBuyProgressVO queryGroupBuyProgress(String teamId);

    MarketPayOrderEntity lockMarketPayOrder(GroupBuyOrderAggregate groupBuyOrderAggregate);

    GroupBuyActivityEntity queryGroupBuyActivityByActivityId(long activityId);

    int queryOrderCountByActivityId(long activityId, String userId);

    GroupBuyTeamEntity queryGroupBuyTeamByTeamId(String teamId);

    void settlementMarketPayOrder(GroupBuyTeamSettlementAggregate groupBuyTeamSettlementAggregate);

}
