package com.hth.domain.activity.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ActivityInfoEntity {
    private Long activityId;
    private String activityName;
    private String discountId;
    private Integer groupType;
    private Integer takeLimitCount;
    private Integer target;
    private Integer validTime;
    private Integer status;
    private String tagId;
    private String tagScope;
    private Date startTime;
    private Date endTime;
    private List<GoodsInfoEntity> goodsInfoEntityList;
    
    /**
     * 生成缓存键 - 查询所有活动
     * @return 缓存键
     */
    public static String cacheRedisKey() {
        return "group_buy_market_all_activities";
    }
}
