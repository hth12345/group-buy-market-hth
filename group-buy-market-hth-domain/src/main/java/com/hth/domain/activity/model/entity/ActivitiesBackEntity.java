package com.hth.domain.activity.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ActivitiesBackEntity {
    private List<ActivityInfoEntity> activityInfoEntityList;
}
