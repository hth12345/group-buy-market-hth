package com.hth.domain.activity.service.discount;

import com.hth.domain.activity.adapter.repository.IActivityRepository;
import com.hth.domain.activity.model.valobj.DiscountTypeEnum;
import com.hth.domain.activity.model.valobj.GroupBuyActivityDiscountVO;

import javax.annotation.Resource;
import java.math.BigDecimal;

public abstract class AbstractDiscountCalculateService implements IDiscountCalculateService{
    @Resource
    private IActivityRepository repository;
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

    // 人群过滤 - 限定人群优惠
    private boolean filterTagId(String userId, String tagId) {
        return repository.isTagCrowdRange(userId, tagId);
    }

    protected abstract BigDecimal doCalculate(BigDecimal originalPrice, GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount);

    // 判断折扣后金额，最低支付1分钱
    protected BigDecimal isLessThanZero(BigDecimal deductionPrice){
        if(deductionPrice.compareTo(BigDecimal.ZERO) <= 0)
            return new BigDecimal("0.01");
        return deductionPrice;
    }
}
