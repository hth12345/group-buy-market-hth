package com.hth.domain.activity.service.filter;

import cn.bugstack.wrench.design.framework.link.model2.handler.ILogicHandler;
import com.hth.domain.activity.adapter.repository.IActivityRepository;
import com.hth.domain.activity.model.entity.ActivitiesBackEntity;
import com.hth.domain.activity.model.entity.ActivitiesCommandEntity;
import com.hth.domain.activity.model.entity.ActivityInfoEntity;
import com.hth.domain.activity.model.valobj.TagScopeEnumVO;
import com.hth.domain.activity.service.factory.ActivitiesRuleFilterFactory;
import com.hth.types.common.Constants;
import com.hth.types.enums.ResponseCode;
import com.hth.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Slf4j
@Service
public class ValidityFilter implements ILogicHandler<ActivitiesCommandEntity, ActivitiesRuleFilterFactory.DynamicContext, ActivitiesBackEntity> {
    @Resource
    private IActivityRepository repository;
    @Override
    public ActivitiesBackEntity apply(ActivitiesCommandEntity requestParameter, ActivitiesRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        String userId = requestParameter.getUserId();
        List<ActivityInfoEntity> allActivitiesList = dynamicContext.getActivityList();
        //定义一个空集合
        List<ActivityInfoEntity> validityActivitiesList = new ArrayList<>();
        for(ActivityInfoEntity activityInfo : allActivitiesList){
            String tagId = activityInfo.getTagId();
            Date startTime = activityInfo.getStartTime();
            Date endTime = activityInfo.getEndTime();
            Date now = new Date();
            if(startTime.after(now) || endTime.before(now)){
                continue;
            }
            if (StringUtils.isBlank(tagId)){
                validityActivitiesList.add(activityInfo);
                continue;
            }
            //活动本身是否设置可见
            boolean isVisible = isVisible(activityInfo.getTagScope());
            boolean isEnable = isEnable(activityInfo.getTagScope());

            //活动是否对该用户可见
            boolean isWithin = repository.isTagCrowdRange(tagId, userId);
            if(isWithin || isVisible || isEnable){
                validityActivitiesList.add(activityInfo);
            }
        }
        dynamicContext.setActivityList(validityActivitiesList);

        return next(requestParameter, dynamicContext);
    }
    private boolean isVisible(String tagScope){
        if(StringUtils.isBlank(tagScope)){
            return TagScopeEnumVO.VISIBLE.getAllow();
        }
        String[] split = tagScope.split(Constants.SPLIT);
        if (split.length > 0 && Objects.equals(split[0],"1") && StringUtils.isNotBlank(split[0])){
            return TagScopeEnumVO.VISIBLE.getRefuse();
        }
        return TagScopeEnumVO.VISIBLE.getAllow();
    }
    public boolean isEnable(String tagScope){
        if(StringUtils.isBlank(tagScope)){
            return TagScopeEnumVO.ENABLE.getAllow();
        }
        String[] split = tagScope.split(Constants.SPLIT);
        if(split.length == 2 && Objects.equals(split[1],"2") && StringUtils.isNotBlank(split[1])){
            return TagScopeEnumVO.ENABLE.getRefuse();
        }
        return TagScopeEnumVO.ENABLE.getAllow();
    }
}
