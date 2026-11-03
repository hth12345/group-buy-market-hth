package com.hth.domain.activity.service.filter;

import cn.bugstack.wrench.design.framework.link.model2.handler.ILogicHandler;
import com.hth.domain.activity.model.entity.ActivitiesBackEntity;
import com.hth.domain.activity.model.entity.ActivitiesCommandEntity;
import com.hth.domain.activity.model.entity.ActivityInfoEntity;
import com.hth.domain.activity.service.factory.ActivitiesRuleFilterFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BackDataFilter implements ILogicHandler<ActivitiesCommandEntity, ActivitiesRuleFilterFactory.DynamicContext, ActivitiesBackEntity> {
    @Override
    public ActivitiesBackEntity apply(ActivitiesCommandEntity activitiesCommandEntity, ActivitiesRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        List<ActivityInfoEntity> validityActivitiesList = dynamicContext.getActivityList();

        return ActivitiesBackEntity.builder()
                .activityInfoEntityList(validityActivitiesList)
                .build();
    }
}
