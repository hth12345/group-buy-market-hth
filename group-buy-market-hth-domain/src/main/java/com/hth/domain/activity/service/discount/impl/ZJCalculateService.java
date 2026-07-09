package com.hth.domain.activity.service.discount.impl;

import com.hth.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import com.hth.domain.activity.service.discount.AbstractDiscountCalculateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
@Slf4j
@Service("ZJ")
public class ZJCalculateService extends AbstractDiscountCalculateService {
    @Override
    protected BigDecimal doCalculate(BigDecimal originalPrice, GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount) {
        // 折扣表达式 - 直减为扣减金额
        String marketExpr = groupBuyDiscount.getMarketExpr();
        // 折扣价格
        BigDecimal deductionPrice = originalPrice.subtract(new BigDecimal(marketExpr));

        return isLessThanZero(deductionPrice);
    }
}
