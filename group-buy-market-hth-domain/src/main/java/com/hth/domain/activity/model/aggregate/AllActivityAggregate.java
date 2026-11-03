package com.hth.domain.activity.model.aggregate;

import com.hth.domain.activity.model.entity.ActivityInfoEntity;
import com.hth.domain.activity.model.entity.GoodsInfoEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AllActivityAggregate {
    private ActivityInfoEntity activityInfoEntity;
    private GoodsInfoEntity goodsInfoEntity;
}
