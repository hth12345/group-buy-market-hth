package com.hth.domain.activity.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 商品信息
 * @create 2024-12-21 10:45
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SCSkuActivityVO {

    /** 渠道 */
    private String source;
    /** 来源 */
    private String chanel;
    /** 活动ID */
    private Long activityId;
    /** 商品ID */
    private String goodsId;
}
