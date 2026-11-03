package com.hth.domain.activity.service.filter;

import cn.bugstack.wrench.design.framework.link.model2.handler.ILogicHandler;
import com.hth.domain.activity.adapter.repository.IActivityRepository;
import com.hth.domain.activity.model.entity.ActivitiesBackEntity;
import com.hth.domain.activity.model.entity.ActivitiesCommandEntity;
import com.hth.domain.activity.model.entity.ActivityInfoEntity;
import com.hth.domain.activity.model.entity.GoodsInfoEntity;
import com.hth.domain.activity.service.factory.ActivitiesRuleFilterFactory;
import com.hth.types.enums.ResponseCode;
import com.hth.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class QueryDataFilter implements ILogicHandler<ActivitiesCommandEntity, ActivitiesRuleFilterFactory.DynamicContext, ActivitiesBackEntity> {
    @Resource
    private IActivityRepository repository;
    //查询活动配置写入上下文
    @Override
    public ActivitiesBackEntity apply(ActivitiesCommandEntity requestParameter, ActivitiesRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        List<ActivityInfoEntity> activityList = repository.queryAllActivities();
        if (CollectionUtils.isEmpty(activityList)){
            throw new AppException(ResponseCode.E0108.getCode(), ResponseCode.E0108.getInfo());
        }
        List<Long> activityIds = activityList.stream()
                .map(ActivityInfoEntity::getActivityId)
                .collect(Collectors.toList());
        List<GoodsInfoEntity> goodsList = repository.queryAllGoods(activityIds);
        if (CollectionUtils.isEmpty(goodsList)){
            throw new AppException(ResponseCode.E0109.getCode(), ResponseCode.E0109.getInfo());
        }
        // 将商品按 activityId 分组（一个活动可能关联多个商品）
        Map<Long, List<GoodsInfoEntity>> goodsMap = goodsList.stream()
                .collect(Collectors.groupingBy(GoodsInfoEntity::getActivityId));

        // 将每个活动关联的商品列表塞入活动实体
        for (ActivityInfoEntity activity : activityList) {
            List<GoodsInfoEntity> goodsInfoList = goodsMap.get(activity.getActivityId());
            if (goodsInfoList != null) {
                activity.setGoodsInfoEntityList(goodsInfoList);
            }
        }
        dynamicContext.setActivityList(activityList);
        return next(requestParameter, dynamicContext);
    }
}
