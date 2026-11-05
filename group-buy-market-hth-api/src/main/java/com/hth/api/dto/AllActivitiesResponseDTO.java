package com.hth.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AllActivitiesResponseDTO {
    private Long activityId;
    private String activityName;
    private String discountId;
    private Integer groupType;
    private Integer takeLimitCount;
    private Integer target;
    private Integer validTime;
    private Integer status;
    private Date startTime;
    private Date endTime;
    private List<Goods> goods;

    /**
     * 商品信息
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Goods {
        // 商品ID
        private String goodsId;
        // 商品名称
        private String goodsName;
        // 原始价格
        private BigDecimal originalPrice;
        // 商品图片URL
        private String imageUrl;
    }
}
