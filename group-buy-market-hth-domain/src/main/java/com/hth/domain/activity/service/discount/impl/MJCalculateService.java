package com.hth.domain.activity.service.discount.impl;

import com.hth.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import com.hth.domain.activity.service.discount.AbstractDiscountCalculateService;
import com.hth.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
@Slf4j
@Service("MJ")
public class MJCalculateService extends AbstractDiscountCalculateService {
    @Override
    protected BigDecimal doCalculate(BigDecimal originalPrice, GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount) {
        // 折扣表达式 - 100,10 满100减10元
        String marketExpr = groupBuyDiscount.getMarketExpr();
        String[] split = marketExpr.split(Constants.SPLIT);
        BigDecimal x = new BigDecimal(split[0]);
        BigDecimal y = new BigDecimal(split[1]);
        // 不满足最低满减约束，则按照原价
        if(originalPrice.compareTo(x) < 0){
            return originalPrice;
        }
        // 折扣价格
        BigDecimal deductionPrice = originalPrice.subtract(y);

        return isLessThanZero(deductionPrice);
    }
}
