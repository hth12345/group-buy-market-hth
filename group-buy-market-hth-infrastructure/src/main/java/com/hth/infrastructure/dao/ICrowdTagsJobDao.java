package com.hth.infrastructure.dao;

import com.hth.infrastructure.dao.po.CrowdTagsJob;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author hth
 * @description 人群标签任务
 * @create 2024-12-28 11:50
 */
@Mapper
public interface ICrowdTagsJobDao {

    CrowdTagsJob queryCrowdTagsJob(CrowdTagsJob crowdTagsJobReq);

}
