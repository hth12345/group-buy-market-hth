package com.hth.domain.trade.adapter.port;

import com.hth.domain.trade.model.entity.NotifyTaskEntity;

public interface ITradePort {
    String groupBuyNotify(NotifyTaskEntity notifyTaskEntity) throws Exception;
}
