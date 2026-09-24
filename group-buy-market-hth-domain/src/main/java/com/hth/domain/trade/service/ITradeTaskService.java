package com.hth.domain.trade.service;

import com.hth.domain.trade.model.entity.NotifyTaskEntity;

import java.util.Map;

public interface ITradeTaskService {

    //通过定时执行回调任务
    Map<String, Integer> execNotifyJob() throws Exception;

    //完成拼团后自动执行回调任务
    Map<String, Integer> execNotifyJob(String teamId) throws Exception;

    Map<String, Integer> execNotifyJob(NotifyTaskEntity notifyTaskEntity) throws Exception;

}

