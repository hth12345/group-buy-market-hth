package com.hth.domain.activity.service.discount.impl;

import com.hth.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import com.hth.domain.activity.service.discount.AbstractDiscountCalculateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
@Slf4j
@Service("N")
public class NCalculateService extends AbstractDiscountCalculateService {
    @Override
    protected BigDecimal doCalculate(BigDecimal originalPrice, GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount) {
        // 折扣表达式 - 直接为优惠后的金额
        String marketExpr = groupBuyDiscount.getMarketExpr();
        // n元购
        BigDecimal deductionPrice = new BigDecimal(marketExpr);
        return isLessThanZero(deductionPrice);
    }
}
