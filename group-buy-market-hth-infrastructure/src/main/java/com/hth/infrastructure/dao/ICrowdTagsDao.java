package com.hth.infrastructure.dao;

import com.hth.infrastructure.dao.po.CrowdTags;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author hth
 * @description 人群标签
 * @create 2024-12-28 11:49
 */
@Mapper
public interface ICrowdTagsDao {

    void updateCrowdTagsStatistics(CrowdTags crowdTagsReq);

}
