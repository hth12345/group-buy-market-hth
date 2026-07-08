package com.hth.domain.activity.adapter.repository;

import com.hth.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import com.hth.domain.activity.model.valobj.SkuVO;

public interface IActivityRepository {

    GroupBuyActivityDiscountVO queryGroupBuyActivityDiscountVO(String source,String channel);
    SkuVO querySkuByGoodsId(String goodsId);
}
