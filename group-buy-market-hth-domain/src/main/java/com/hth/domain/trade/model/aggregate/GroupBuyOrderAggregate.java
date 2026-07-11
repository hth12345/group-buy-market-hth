package com.hth.domain.trade.model.aggregate;

import com.hth.domain.trade.model.entity.PayActivityEntity;
import com.hth.domain.trade.model.entity.PayDiscountEntity;
import com.hth.domain.trade.model.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupBuyOrderAggregate {
    private UserEntity userEntity;
    private PayActivityEntity payActivityEntity;
    private PayDiscountEntity payDiscountEntity;
}
