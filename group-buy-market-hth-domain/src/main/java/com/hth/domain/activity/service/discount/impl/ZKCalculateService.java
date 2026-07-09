package com.hth.domain.activity.service.discount.impl;

import com.hth.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import com.hth.domain.activity.service.discount.AbstractDiscountCalculateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
@Slf4j
@Service("ZK")
public class ZKCalculateService extends AbstractDiscountCalculateService {
    @Override
    protected BigDecimal doCalculate(BigDecimal originalPrice, GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount) {
        // 折扣表达式 - 折扣百分比
        String marketExpr = groupBuyDiscount.getMarketExpr();
        // 折扣价格
        BigDecimal deductionPrice = originalPrice.multiply(new BigDecimal(marketExpr));

        return isLessThanZero(deductionPrice);
    }
}
