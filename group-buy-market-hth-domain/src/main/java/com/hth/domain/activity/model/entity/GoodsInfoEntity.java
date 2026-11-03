package com.hth.domain.activity.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GoodsInfoEntity {

    private Long activityId;
    // 商品ID
    private String goodsId;
    // 商品名称
    private String goodsName;
    // 原始价格
    private BigDecimal originalPrice;
    
    /**
     * 生成缓存键 - 根据活动ID列表查询所有商品
     * @param activityIds 活动ID列表
     * @return 缓存键
     */
    public static String cacheRedisKey(List<Long> activityIds) {
        // 将活动ID列表转换为字符串，使用下划线连接
        String activityIdsStr = String.join("_", activityIds.stream()
                .map(String::valueOf)
                .toArray(String[]::new));
        return "group_buy_market_goods_all_" + activityIdsStr;
    }
}
