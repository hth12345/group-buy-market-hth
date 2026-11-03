package com.hth.domain.activity.service.factory;

import cn.bugstack.wrench.design.framework.link.model2.LinkArmory;
import cn.bugstack.wrench.design.framework.link.model2.chain.BusinessLinkedList;
import com.hth.domain.activity.model.entity.ActivitiesBackEntity;
import com.hth.domain.activity.model.entity.ActivitiesCommandEntity;
import com.hth.domain.activity.model.entity.ActivityInfoEntity;
import com.hth.domain.activity.service.filter.BackDataFilter;
import com.hth.domain.activity.service.filter.QueryDataFilter;
import com.hth.domain.activity.service.filter.SCRulerFilter;
import com.hth.domain.activity.service.filter.ValidityFilter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
@Configuration
public class ActivitiesRuleFilterFactory {
    @Bean("activityRuleFilter")
    public BusinessLinkedList<ActivitiesCommandEntity, ActivitiesRuleFilterFactory.DynamicContext, ActivitiesBackEntity> tradeRuleFilter(
            SCRulerFilter scRulerFilter,
            QueryDataFilter queryDataFilter,
            ValidityFilter validityFilter,
            BackDataFilter backDataFilter) {
        // 组装链
        LinkArmory<ActivitiesCommandEntity, ActivitiesRuleFilterFactory.DynamicContext, ActivitiesBackEntity> linkArmory =
                new LinkArmory<>("活动显示过滤链",
                        scRulerFilter,
                        queryDataFilter,
                        validityFilter,
                        backDataFilter);

        // 链对象
        return linkArmory.getLogicLink();
    }


    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DynamicContext {
        private String userId;
        private String source;
        private String channel;
        private List<ActivityInfoEntity> activityList;
        private List<ActivityInfoEntity> validityActivitiesList;
    }
}
