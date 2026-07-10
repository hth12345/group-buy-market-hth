package com.hth.config;

import com.hth.types.annotations.DCCValue;
import com.hth.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBucket;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
public class DCCValueBeanFactory implements BeanPostProcessor {

    private static final String BASE_CONFIG_PATH ="group_buy_market_dcc_";

    private final RedissonClient redissonClient;

    private final Map<String,Object> dccObject = new HashMap<>();

    public DCCValueBeanFactory(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Bean("dccTopic")
    public RTopic dccRedisTopicListener(RedissonClient redissonClient) {
        RTopic topic = redissonClient.getTopic("group_buy_market_dcc");
        topic.addListener(String.class, new MessageListener<String>() {
            @Override
            public void onMessage(CharSequence charSequence, String s) {
                String[] split = s.split(Constants.SPLIT);
                // 获取值
                String attribute = split[0];
                String key = BASE_CONFIG_PATH + attribute;
                String value = split[1];

                // 设置值
                RBucket<String> bucket = redissonClient.getBucket(key);
                boolean exists = bucket.isExists();
                if(!exists){
                    return;
                }
                bucket.set(value);
                Object objBean = dccObject.get(key);
                if(objBean == null){
                    return;
                }
                Class<?> objBeanClass = objBean.getClass();
                // 检查 objBean 是否是代理对象
                if(AopUtils.isAopProxy(objBean)){
                    // 获取代理对象的目标对象
                    objBeanClass = AopUtils.getTargetClass(objBean);
                }
                try {
                    // 1. getDeclaredField 方法用于获取指定类中声明的所有字段，包括私有字段、受保护字段和公共字段。
                    // 2. getField 方法用于获取指定类中的公共字段，即只能获取到公共访问修饰符（public）的字段。
                    Field field = objBeanClass.getDeclaredField(attribute);
                    field.setAccessible(true);
                    field.set(objBean, value);
                    field.setAccessible(false);
                } catch (Exception e) {
                    throw new RuntimeException("...");
                }
            }
        });
        return topic;

    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // 注意；增加 AOP 代理后，获得类的方式要通过 AopProxyUtils.getTargetClass(bean); 不能直接 bean.class 因为代理后类的结构发生变化，这样不能获得到自己的自定义注解了。
        Class<?> targetBeanClass = bean.getClass();
        Object targetBeanObject = bean;
        if(AopUtils.isAopProxy(bean)){
            targetBeanClass = AopUtils.getTargetClass(bean);
            targetBeanObject = AopProxyUtils.getSingletonTarget(bean);
        }
        Field[] fields = targetBeanClass.getDeclaredFields();
        for (Field field : fields) {
            if(!field.isAnnotationPresent(DCCValue.class)){
                continue;
            }
            DCCValue dccValue = field.getAnnotation(DCCValue.class);
            String value = dccValue.value();
            if(StringUtils.isBlank(value)){
                throw new RuntimeException("...");
            }
            String[] split = value.split(":");
            String key = BASE_CONFIG_PATH.concat(split[0]);
            String defaultValue = split.length == 2 ? split[1] : null;

            // 设置值
            String setValue = defaultValue;

            try {
                // 如果为空则抛出异常
                if(StringUtils.isBlank(defaultValue)){
                    throw new RuntimeException("...");
                }
                // Redis 操作，判断配置Key是否存在，不存在则创建，存在则获取最新值
                RBucket<String> bucket = redissonClient.getBucket(key);
                boolean exists = bucket.isExists();
                if(!exists){
                    bucket.set(defaultValue);
                }else {
                    setValue = bucket.get();
                }
                field.setAccessible(true);
                field.set(targetBeanObject, setValue);
                field.setAccessible(false);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("...");
            }
            dccObject.put(key, targetBeanObject);
        }

        return bean;
    }
}
