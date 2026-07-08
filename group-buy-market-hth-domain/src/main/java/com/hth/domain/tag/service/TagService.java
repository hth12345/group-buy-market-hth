package com.hth.domain.tag.service;

import com.hth.domain.tag.adapter.repository.ITagRepository;
import com.hth.domain.tag.model.entity.CrowdTagsJobEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class TagService implements ITagService {
    @Resource
    private ITagRepository repository;

    @Override
    public void execTagBatchJob(String tagId, String batchId) {
        CrowdTagsJobEntity crowdTagsJobEntity = repository.queryCrowdTagsJobEntity(tagId,batchId);

        List<String> userIdList = new ArrayList<String>(){{
            add("xiaofuge");
            add("zhangsan");
        }};

        for (String userId : userIdList) {
            repository.addCrowdTagsUserId(tagId,userId);
        }

        repository.updateCrowdTagsStatistics(tagId,userIdList.size());
    }
}
