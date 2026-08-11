package com.hth.infrastructure.adapter.repository;

import com.hth.infrastructure.dcc.DCCService;
import com.hth.infrastructure.redis.IRedisService;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public abstract class AbstractRepository {
    private final Logger logger = LoggerFactory.getLogger(AbstractRepository.class);
    @Resource
    protected IRedisService redisService;
    @Resource
    protected DCCService dccService;

    protected <T> T getFromCacheOrDb(String cacheKey, Supplier<T> dbFallBack, Long time){
        //判断是否开启缓存
        if(dccService.isCacheOpenSwitch()){
            //从缓存中获取值
            T cacheResult = redisService.getValue(cacheKey);

            //缓存有值直接返回缓存中的值
            if(null != cacheResult) {return cacheResult;}
            //缓没有值从数据库中查找
            T dbResult = dbFallBack.get();

            //判断数据库中的值是否为空
            if(null == dbResult) {return null;}

            //将数据库中的值写入缓存（带过期时间）
            redisService.setValue(cacheKey,dbResult,time);

            return dbResult;
        } else {
            //未开启缓存直接查库
            logger.warn("缓存降级 {}", cacheKey);
            return dbFallBack.get();
        }
    }
}
