package com.hth.test.infrastructure.dao;

import com.alibaba.fastjson.JSON;
import com.hth.infrastructure.dao.IGroupBuyActivityDao;
import com.hth.infrastructure.dao.po.GroupBuyActivity;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class GroupBuyActivityDaoTest {
    @Autowired
    private IGroupBuyActivityDao iGroupBuyActivityDao;

    @Test
    public void test(){
        List<GroupBuyActivity> groupBuyActivities = iGroupBuyActivityDao.queryGroupBuyActivityList();
        log.info("测试结果：{}", JSON.toJSONString(groupBuyActivities));
    }
}
