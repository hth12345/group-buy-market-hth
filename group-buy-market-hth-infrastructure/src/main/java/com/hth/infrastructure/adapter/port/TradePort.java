package com.hth.infrastructure.adapter.port;

import com.hth.domain.trade.adapter.port.ITradePort;
import com.hth.domain.trade.model.entity.NotifyTaskEntity;
import com.hth.domain.trade.model.valobj.NotifyTypeEnumVO;
import com.hth.infrastructure.event.EventPublisher;
import com.hth.infrastructure.gateway.GroupBuyNotifyService;
import com.hth.infrastructure.redis.IRedisService;
import com.hth.types.enums.NotifyTaskHTTPEnumVO;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Service
public class TradePort implements ITradePort {
    @Resource
    private GroupBuyNotifyService groupBuyNotifyService;
    @Resource
    private IRedisService redisService;
    @Resource
    private EventPublisher publisher;
    @Override
    public String groupBuyNotify(NotifyTaskEntity notifyTaskEntity) throws Exception {
        RLock lock = redisService.getLock(notifyTaskEntity.lockKey());
        try{
            if(lock.tryLock(3,0, TimeUnit.SECONDS)){
                try {
                    //HTTP
                    if (notifyTaskEntity.getNotifyType().equals(NotifyTypeEnumVO.HTTP.getCode())) {
                        if (StringUtils.isBlank(notifyTaskEntity.getNotifyUrl()) || "暂无".equals(notifyTaskEntity.getNotifyUrl())) {
                            return NotifyTaskHTTPEnumVO.SUCCESS.getCode();
                        }
                        return groupBuyNotifyService.groupBuyNotify(notifyTaskEntity.getNotifyUrl(), notifyTaskEntity.getParameterJson());
                    }
                    //MQ
                    if (notifyTaskEntity.getNotifyType().equals(NotifyTypeEnumVO.MQ.getCode())){
                        publisher.publish(notifyTaskEntity.getNotifyMQ(),notifyTaskEntity.getParameterJson());
                        return NotifyTaskHTTPEnumVO.SUCCESS.getCode();
                    }
                }

                finally {
                    if(lock.isLocked() && lock.isHeldByCurrentThread()){
                        lock.unlock();
                    }
                }
            }
            return NotifyTaskHTTPEnumVO.NULL.getCode();
        }catch (Exception e){
            Thread.currentThread().interrupt();
        }
        return NotifyTaskHTTPEnumVO.NULL.getCode();
    }
}
