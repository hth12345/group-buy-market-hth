package com.hth.domain.tag.adapter.repository;

import com.hth.domain.tag.model.entity.CrowdTagsJobEntity;

public interface ITagRepository {

    CrowdTagsJobEntity queryCrowdTagsJobEntity(String tagId, String batchId);

    void addCrowdTagsUserId(String tagId, String userId);

    void updateCrowdTagsStatistics(String tagId, int size);

}
