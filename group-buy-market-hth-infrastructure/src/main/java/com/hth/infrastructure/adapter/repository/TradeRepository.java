package com.hth.infrastructure.adapter.repository;

import com.alibaba.fastjson2.JSON;
import com.esotericsoftware.minlog.Log;
import com.hth.domain.trade.adapter.repository.ITradeRepository;
import com.hth.domain.trade.model.aggregate.GroupBuyOrderAggregate;
import com.hth.domain.trade.model.aggregate.GroupBuyRefundAggregate;
import com.hth.domain.trade.model.aggregate.GroupBuyTeamSettlementAggregate;
import com.hth.domain.trade.model.entity.*;
import com.hth.domain.trade.model.valobj.GroupBuyProgressVO;
import com.hth.domain.trade.model.valobj.NotifyConfigVO;
import com.hth.domain.trade.model.valobj.NotifyTypeEnumVO;
import com.hth.domain.trade.model.valobj.TradeOrderStatusEnumVO;
import com.hth.infrastructure.dao.IGroupBuyActivityDao;
import com.hth.infrastructure.dao.IGroupBuyOrderDao;
import com.hth.infrastructure.dao.IGroupBuyOrderListDao;
import com.hth.infrastructure.dao.INotifyTaskDao;
import com.hth.infrastructure.dao.po.GroupBuyActivity;
import com.hth.infrastructure.dao.po.GroupBuyOrder;
import com.hth.infrastructure.dao.po.GroupBuyOrderList;
import com.hth.infrastructure.dao.po.NotifyTask;
import com.hth.infrastructure.dcc.DCCService;
import com.hth.infrastructure.redis.IRedisService;
import com.hth.types.common.Constants;
import com.hth.types.enums.ActivityStatusEnumVO;
import com.hth.types.enums.GroupBuyOrderEnumVO;
import com.hth.types.enums.ResponseCode;
import com.hth.types.exception.AppException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Repository
public class TradeRepository implements ITradeRepository {
    @Resource
    private IGroupBuyOrderDao groupBuyOrderDao;
    @Resource
    private IGroupBuyOrderListDao groupBuyOrderListDao;
    @Resource
    private IGroupBuyActivityDao groupBuyActivityDao;
    @Resource
    private INotifyTaskDao notifyTaskDao;
    @Resource
    private DCCService dccService;
    @Resource
    private IRedisService redisService;
    @Value("${spring.rabbitmq.config.producer.topic_team_success.routing_key}")
    private String topic_team_success;
    @Autowired
    private IGroupBuyOrderListDao iGroupBuyOrderListDao;

    @Override
    public MarketPayOrderEntity queryNoPayMarketPayOrderByOutTradeNo(String userId, String outTradeNo) {
        GroupBuyOrderList groupBuyOrderListReq = new GroupBuyOrderList();
        groupBuyOrderListReq.setOutTradeNo(outTradeNo);
        groupBuyOrderListReq.setUserId(userId);
        GroupBuyOrderList groupBuyOrderList = groupBuyOrderListDao.queryGroupBuyOrderRecordByOutTradeNo(groupBuyOrderListReq);
        if (groupBuyOrderList == null) {
            return null;
        }
        return MarketPayOrderEntity.builder()
                .teamId(groupBuyOrderList.getTeamId())
                .orderId(groupBuyOrderList.getOrderId())
                .originalPrice(groupBuyOrderList.getOriginalPrice())
                .deductionPrice(groupBuyOrderList.getDeductionPrice())
                .payPrice(groupBuyOrderList.getPayPrice())
                .tradeOrderStatusEnumVO(TradeOrderStatusEnumVO.valueOf(groupBuyOrderList.getStatus()))
                .build();
    }

    @Override
    public GroupBuyProgressVO queryGroupBuyProgress(String teamId) {
        GroupBuyOrder groupBuyOrder = groupBuyOrderDao.queryGroupBuyProgress(teamId);
        if (groupBuyOrder == null) {
            return null;
        }
        return GroupBuyProgressVO.builder()
                .targetCount(groupBuyOrder.getTargetCount())
                .completeCount(groupBuyOrder.getCompleteCount())
                .lockCount(groupBuyOrder.getLockCount())
                .build();
    }

    @Transactional(timeout = 500)
    @Override
    public MarketPayOrderEntity lockMarketPayOrder(GroupBuyOrderAggregate groupBuyOrderAggregate) {
        // 聚合对象信息
        PayActivityEntity payActivityEntity = groupBuyOrderAggregate.getPayActivityEntity();
        UserEntity userEntity = groupBuyOrderAggregate.getUserEntity();
        PayDiscountEntity payDiscountEntity = groupBuyOrderAggregate.getPayDiscountEntity();
        Integer validTime = groupBuyOrderAggregate.getValidTime();
        //用户参与活动的次数
        Integer userTakeOrderCount = groupBuyOrderAggregate.getUserTakeOrderCount();
        // 判断是否有团 - teamId 为空 - 新团、为不空 - 老团
        String teamId = payActivityEntity.getTeamId();
        if(StringUtils.isBlank(teamId)){
            teamId = RandomStringUtils.randomAlphanumeric(8);
            //构建拼团订单
            Calendar calendar = Calendar.getInstance();
            Date validStartTime = new Date();
            calendar.setTime(validStartTime);
            calendar.add(Calendar.MINUTE, validTime);
            Date validEndTime = calendar.getTime();
            GroupBuyOrder groupBuyOrder = GroupBuyOrder.builder()
                    .teamId(teamId)
                    .activityId(payActivityEntity.getActivityId())
                    .source(payDiscountEntity.getSource())
                    .channel(payDiscountEntity.getChannel())
                    .originalPrice(payDiscountEntity.getOriginalPrice())
                    .deductionPrice(payDiscountEntity.getDeductionPrice())
                    .payPrice(payDiscountEntity.getPayPrice())
                    .targetCount(payActivityEntity.getTargetCount())
                    .completeCount(0)
                    .lockCount(1)
                    .validStartTime(validStartTime)  // 拼团开始时间
                    .validEndTime(validEndTime)      // 拼团结束时间
                    .notifyType(String.valueOf(payDiscountEntity.getNotifyConfigVO().getNotifyType()))    // 回调类型，默认为 HTTP
                    .notifyUrl(payDiscountEntity.getNotifyConfigVO().getNotifyUrl())         // 回调地址，可为空字符串
                    .build();
            // 写入记录
            groupBuyOrderDao.insert(groupBuyOrder);

        }
        else {
            // 更新记录 - 如果更新记录不等于1，则表示拼团已满，抛出异常
            int updateAddTargetCount = groupBuyOrderDao.updateAddLockCount(teamId);
            if(updateAddTargetCount != 1){
                throw new AppException(ResponseCode.E0006.getCode());
            }
        }

        String orderId = RandomStringUtils.randomAlphanumeric(12);
        GroupBuyOrderList groupBuyOrderListReq = GroupBuyOrderList.builder()
                .userId(userEntity.getUserId())
                .teamId(teamId)
                .orderId(orderId)
                .activityId(payActivityEntity.getActivityId())
                .startTime(payActivityEntity.getStartTime())
                .endTime(payActivityEntity.getEndTime())
                .goodsId(payDiscountEntity.getGoodsId())
                .source(payDiscountEntity.getSource())
                .channel(payDiscountEntity.getChannel())
                .originalPrice(payDiscountEntity.getOriginalPrice())
                .deductionPrice(payDiscountEntity.getDeductionPrice())
                .status(TradeOrderStatusEnumVO.CREATE.getCode())
                .outTradeNo(payDiscountEntity.getOutTradeNo())
                .payPrice(payDiscountEntity.getDeductionPrice())  // 支付价格 = 抵扣后价格
                // 构建 bizId 唯一值；活动id_用户id_参与次数累加
                .bizId(payActivityEntity.getActivityId()+ Constants.UNDERLINE + userEntity.getUserId()+ Constants.UNDERLINE +(userTakeOrderCount+1) )
                .build();
        try {
            // 写入拼团记录
            groupBuyOrderListDao.insert(groupBuyOrderListReq);
        } catch (DuplicateKeyException e) {
            throw new AppException(ResponseCode.INDEX_EXCEPTION.getCode());
        }
        return MarketPayOrderEntity.builder()
                .teamId(teamId)
                .orderId(orderId)
                .originalPrice(payDiscountEntity.getOriginalPrice())
                .deductionPrice(payDiscountEntity.getDeductionPrice())
                .payPrice(payDiscountEntity.getPayPrice())
                .tradeOrderStatusEnumVO(TradeOrderStatusEnumVO.CREATE)
                .build();
    }

    //根据活动ID查询活动
    @Override
    public GroupBuyActivityEntity queryGroupBuyActivityByActivityId(long activityId) {
        GroupBuyActivity groupBuyActivity = groupBuyActivityDao.queryGroupBuyActivityByActivityId(activityId);
        if (null == groupBuyActivity) {
            return null;
        }
        return GroupBuyActivityEntity.builder()
                .activityId(groupBuyActivity.getActivityId())
                .activityName(groupBuyActivity.getActivityName())
                .discountId(groupBuyActivity.getDiscountId())
                .groupType(groupBuyActivity.getGroupType())
                .takeLimitCount(groupBuyActivity.getTakeLimitCount())
                .target(groupBuyActivity.getTarget())
                .validTime(groupBuyActivity.getValidTime())
                .status(ActivityStatusEnumVO.valueOf(groupBuyActivity.getStatus()))
                .startTime(groupBuyActivity.getStartTime())
                .endTime(groupBuyActivity.getEndTime())
                .tagId(groupBuyActivity.getTagId())
                .tagScope(groupBuyActivity.getTagScope())
                .build();
    }

    //根据活动ID和用户ID查询订单数量
    @Override
    public int queryOrderCountByActivityId(long activityId, String userId) {
        GroupBuyOrderList groupBuyOrderListReq = new GroupBuyOrderList();
        groupBuyOrderListReq.setActivityId(activityId);
        groupBuyOrderListReq.setUserId(userId);
        return groupBuyOrderListDao.queryOrderCountByActivityId(groupBuyOrderListReq);
    }

    //根据组队ID查询订单
    @Override
    public GroupBuyTeamEntity queryGroupBuyTeamByTeamId(String teamId) {
        GroupBuyOrder groupBuyOrder = groupBuyOrderDao.queryGroupBuyTeamByTeamId(teamId);
        if (groupBuyOrder == null) {
            return null;  // 或者抛出异常
        }
        Integer status = groupBuyOrder.getStatus();
        return GroupBuyTeamEntity.builder()
                .teamId(groupBuyOrder.getTeamId())
                .activityId(groupBuyOrder.getActivityId())
                .targetCount(groupBuyOrder.getTargetCount())
                .completeCount(groupBuyOrder.getCompleteCount())
                .lockCount(groupBuyOrder.getLockCount())
                .status(null == status ? GroupBuyOrderEnumVO.PROGRESS : GroupBuyOrderEnumVO.valueOf(status))
                .validStartTime(groupBuyOrder.getValidStartTime())
                .validEndTime(groupBuyOrder.getValidEndTime())
                .notifyConfigVO(NotifyConfigVO.builder()
                        .notifyType(NotifyTypeEnumVO.valueOf(groupBuyOrder.getNotifyType()))
                        .notifyMQ(topic_team_success)
                        .notifyUrl(groupBuyOrder.getNotifyUrl())
                        .build())
                .build();

    }

    //拼团交易结算
    @Override
    @Transactional(timeout = 500)
    public NotifyTaskEntity settlementMarketPayOrder(GroupBuyTeamSettlementAggregate groupBuyTeamSettlementAggregate) {
        UserEntity userEntity = groupBuyTeamSettlementAggregate.getUserEntity();
        GroupBuyTeamEntity groupBuyTeamEntity = groupBuyTeamSettlementAggregate.getGroupBuyTeamEntity();
        TradePaySuccessEntity tradePaySuccessEntity = groupBuyTeamSettlementAggregate.getTradePaySuccessEntity();
        // 1. 更新拼团订单明细状态
        GroupBuyOrderList groupBuyOrderListReq = new GroupBuyOrderList();
        groupBuyOrderListReq.setUserId(userEntity.getUserId());
        groupBuyOrderListReq.setOutTradeNo(tradePaySuccessEntity.getOutTradeNo());
        groupBuyOrderListReq.setOutTradeTime(tradePaySuccessEntity.getOutTradeTime());

        int updateOrderListStatusCount = groupBuyOrderListDao.updateOrderStatus2COMPLETE(groupBuyOrderListReq);
        if (updateOrderListStatusCount != 1){
            throw new AppException(ResponseCode.UPDATE_ZERO.getCode());
        }

        // 2. 更新拼团达成数量
        String teamId = groupBuyTeamEntity.getTeamId();
        int updateAddCount = groupBuyOrderDao.updateAddCompleteCount(teamId);
        if (updateAddCount != 1){
            throw new AppException(ResponseCode.UPDATE_ZERO.getCode());
        }

        // 3. 更新拼团完成状态
        Integer targetCount = groupBuyTeamEntity.getTargetCount();
        Integer completeCount = groupBuyTeamEntity.getCompleteCount();
        NotifyConfigVO notifyConfigVO = groupBuyTeamEntity.getNotifyConfigVO();
        if(null != targetCount && null != completeCount && targetCount - completeCount == 1){
            int updateOrderStatusCount = groupBuyOrderDao.updateOrderStatus2COMPLETE(teamId);
            if (updateOrderStatusCount != 1){
                throw new AppException(ResponseCode.UPDATE_ZERO.getCode());
            }

            // 查询拼团交易完成外部单号列表
            List<String> outTradeNoList = groupBuyOrderListDao.queryGroupBuyCompleteOrderOutTradeNoListByTeamId(teamId);

            // 拼团完成写入回调任务记录
            NotifyTask notifyTask = new NotifyTask();
            notifyTask.setActivityId(groupBuyTeamEntity.getActivityId());
            notifyTask.setTeamId(groupBuyTeamEntity.getTeamId());
            notifyTask.setNotifyCategory("trade_unpaid2refund");  // 添加：回调种类
            notifyTask.setNotifyType(notifyConfigVO.getNotifyType().getCode());// 添加：回调类型
            notifyTask.setNotifyMQ(notifyConfigVO.getNotifyType().equals(NotifyTypeEnumVO.MQ)?notifyConfigVO.getNotifyMQ():null);
            notifyTask.setNotifyUrl(notifyConfigVO.getNotifyType().equals(NotifyTypeEnumVO.HTTP)? notifyConfigVO.getNotifyUrl() : null);
            notifyTask.setNotifyCount(0);
            notifyTask.setNotifyStatus(0);
            notifyTask.setUuid(java.util.UUID.randomUUID().toString());
            notifyTask.setParameterJson(JSON.toJSONString(new HashMap<String, Object>() {{
                put("teamId", groupBuyTeamEntity.getTeamId());
                put("outTradeNoList", outTradeNoList);
            }}));

            notifyTaskDao.insert(notifyTask);


            return NotifyTaskEntity.builder()
                    .teamId(notifyTask.getTeamId())
                    .notifyUrl(notifyTask.getNotifyUrl())
                    .notifyType(notifyTask.getNotifyType())
                    .notifyMQ(notifyTask.getNotifyMQ())
                    .notifyCount(notifyTask.getNotifyCount())
                    .parameterJson(notifyTask.getParameterJson())
                    .uuid(notifyTask.getUuid())
                    .build();
        }

        return null;
    }

    //判断SC是否在黑名单里
    @Override
    public boolean isSCBlackIntercept(String source, String channel) {
        return dccService.isSCBlackIntercept(source,channel);
    }

    //通过活动ID查询活动拼团有效时间
    @Override
    public Integer queryActivityValidTimeByActivityId(Long activityId) {
        GroupBuyActivity groupBuyActivity = groupBuyActivityDao.queryGroupBuyActivityByActivityId(activityId);
        return groupBuyActivity.getValidTime();
    }

    @Override
    public List<NotifyTaskEntity> queryUnExecutedNotifyTaskList() {
        List<NotifyTask> notifyTaskList = notifyTaskDao.queryUnExecutedNotifyTaskList();
        if (notifyTaskList.isEmpty()) return new ArrayList<>();

        List<NotifyTaskEntity> notifyTaskEntities = new ArrayList<>();
        for (NotifyTask notifyTask : notifyTaskList) {

            NotifyTaskEntity notifyTaskEntity = NotifyTaskEntity.builder()
                    .teamId(notifyTask.getTeamId())
                    .notifyUrl(notifyTask.getNotifyUrl())
                    .notifyCount(notifyTask.getNotifyCount())
                    .parameterJson(notifyTask.getParameterJson())
                    .uuid(notifyTask.getUuid())
                    .build();

            notifyTaskEntities.add(notifyTaskEntity);
        }

        return notifyTaskEntities;
    }

    @Override
    public List<NotifyTaskEntity> queryUnExecutedNotifyTaskList(String teamId) {
        NotifyTask notifyTask = notifyTaskDao.queryUnExecutedNotifyTaskByTeamId(teamId);
        if (null == notifyTask) return new ArrayList<>();
        return Collections.singletonList(NotifyTaskEntity.builder()
                .teamId(notifyTask.getTeamId())
                .notifyUrl(notifyTask.getNotifyUrl())
                .notifyCount(notifyTask.getNotifyCount())
                .parameterJson(notifyTask.getParameterJson())
                .uuid(notifyTask.getUuid())
                .build());
    }

    @Override
    public int updateNotifyTaskStatusSuccess(NotifyTaskEntity notifyTaskEntity) {
        NotifyTask notifyTask = new NotifyTask();
        notifyTask.setTeamId(notifyTaskEntity.getTeamId());
        notifyTask.setUuid(notifyTaskEntity.getUuid());
        return notifyTaskDao.updateNotifyTaskStatusSuccess(notifyTask);
    }

    @Override
    public int updateNotifyTaskStatusError(NotifyTaskEntity notifyTaskEntity) {
        NotifyTask notifyTask = new NotifyTask();
        notifyTask.setTeamId(notifyTaskEntity.getTeamId());
        notifyTask.setUuid(notifyTaskEntity.getUuid());
        return notifyTaskDao.updateNotifyTaskStatusError(notifyTask);
    }

    @Override
    public int updateNotifyTaskStatusRetry(NotifyTaskEntity notifyTaskEntity) {
        NotifyTask notifyTask = new NotifyTask();
        notifyTask.setTeamId(notifyTaskEntity.getTeamId());
        notifyTask.setUuid(notifyTaskEntity.getUuid());
        return notifyTaskDao.updateNotifyTaskStatusRetry(notifyTask);
    }

    @Override
    public boolean occupyTeamStock(String teamStockKey, String recoveryTeamStockKey, Integer target, Integer validTime) {
        Long recoverCount = redisService.getAtomicLong(recoveryTeamStockKey);
        long occupy = redisService.incr(teamStockKey) + 1;
        if(occupy > recoverCount + target){
            redisService.setAtomicLong(teamStockKey,target);
            return false;
        }
        String lockKey = teamStockKey + Constants.UNDERLINE + occupy;
        // TODO 高效整洁
        Boolean isLock = redisService.setNx(lockKey, validTime + 60, TimeUnit.MINUTES);
        if(!isLock){
            Log.info("组队库存加锁失败 {}",lockKey);
        }
        return isLock;
    }

    @Override
    public void recoveryTeamStock(String recoveryTeamStockKey, Integer validTime) {
        if(StringUtils.isBlank(recoveryTeamStockKey)) return;
        redisService.incr(recoveryTeamStockKey);
    }

    @Override
    @Transactional(timeout = 5000)
    public void unpaid2Refund(GroupBuyRefundAggregate groupBuyRefundAggregate) {
        //从聚合函数中取出数据
        TradeRefundOrderEntity tradeRefundOrderEntity = groupBuyRefundAggregate.getTradeRefundOrderEntity();
        GroupBuyProgressVO groupBuyProgress = groupBuyRefundAggregate.getGroupBuyProgress();
        //更新拼团明细表
        GroupBuyOrderList groupBuyOrderListReq = new GroupBuyOrderList();
        groupBuyOrderListReq.setUserId(tradeRefundOrderEntity.getUserId());
        groupBuyOrderListReq.setOrderId(tradeRefundOrderEntity.getOrderId());
        int updateUnpaid2RefundCount = groupBuyOrderListDao.unpaid2Refund(groupBuyOrderListReq);
        if(updateUnpaid2RefundCount != 1){
            throw new AppException(ResponseCode.UPDATE_ZERO.getCode());
        }
        //更新拼团组队表
        GroupBuyOrder groupBuyOrderReq = new GroupBuyOrder();
        groupBuyOrderReq.setTeamId(tradeRefundOrderEntity.getTeamId());
        groupBuyOrderReq.setLockCount(groupBuyProgress.getLockCount());
        int updateTeamUnpaid2RefundCount = groupBuyOrderDao.unpaid2Refund(groupBuyOrderReq);
        if (updateTeamUnpaid2RefundCount != 1){
            throw new AppException(ResponseCode.UPDATE_ZERO.getCode());
        }
    }
}
