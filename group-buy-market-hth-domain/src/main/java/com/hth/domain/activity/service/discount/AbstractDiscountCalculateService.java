package com.hth.domain.activity.service.discount;

import com.hth.domain.activity.model.valobj.DiscountTypeEnum;
import com.hth.domain.activity.model.valobj.GroupBuyActivityDiscountVO;

import java.math.BigDecimal;

public abstract class AbstractDiscountCalculateService implements IDiscountCalculateService{
    @Override
    public BigDecimal calculate(String userId, BigDecimal originalPrice, GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount) {
        if(DiscountTypeEnum.TAG.equals(groupBuyDiscount.getDiscountType())){
            boolean isCrawRange = filterTagId(userId,groupBuyDiscount.getTagId());
            if(!isCrawRange){
                return originalPrice;
            }
        }
        return doCalculate(originalPrice,groupBuyDiscount);
    }

    private boolean filterTagId(String userId, String tagId) {
        return true;
    }

    protected abstract BigDecimal doCalculate(BigDecimal originalPrice, GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount);

    protected BigDecimal isLessThanZero(BigDecimal deductionPrice){
        if(deductionPrice.compareTo(BigDecimal.ZERO) <= 0)
            return new BigDecimal("0.01");
        return deductionPrice;
    }
}
