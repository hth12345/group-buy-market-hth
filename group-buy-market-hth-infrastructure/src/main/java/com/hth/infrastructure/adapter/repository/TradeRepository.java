package com.hth.infrastructure.adapter.repository;

import com.hth.domain.trade.adapter.repository.ITradeRepository;
import com.hth.domain.trade.model.aggregate.GroupBuyOrderAggregate;
import com.hth.domain.trade.model.entity.*;
import com.hth.domain.trade.model.valobj.GroupBuyProgressVO;
import com.hth.domain.trade.model.valobj.TradeOrderStatusEnumVO;
import com.hth.infrastructure.dao.IGroupBuyActivityDao;
import com.hth.infrastructure.dao.IGroupBuyOrderDao;
import com.hth.infrastructure.dao.IGroupBuyOrderListDao;
import com.hth.infrastructure.dao.po.GroupBuyActivity;
import com.hth.infrastructure.dao.po.GroupBuyOrder;
import com.hth.infrastructure.dao.po.GroupBuyOrderList;
import com.hth.types.common.Constants;
import com.hth.types.enums.ActivityStatusEnumVO;
import com.hth.types.enums.ResponseCode;
import com.hth.types.exception.AppException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Repository
public class TradeRepository implements ITradeRepository {
    @Resource
    private IGroupBuyOrderDao groupBuyOrderDao;
    @Resource
    private IGroupBuyOrderListDao groupBuyOrderListDao;
    @Resource
    private IGroupBuyActivityDao groupBuyActivityDao;
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
                .orderId(groupBuyOrderList.getOrderId())
                .deductionPrice(groupBuyOrderList.getDeductionPrice())
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
        //用户参与活动的次数
        Integer userTakeOrderCount = groupBuyOrderAggregate.getUserTakeOrderCount();
        // 判断是否有团 - teamId 为空 - 新团、为不空 - 老团
        String teamId = payActivityEntity.getTeamId();
        if(StringUtils.isBlank(teamId)){
            teamId = RandomStringUtils.randomAlphanumeric(8);
            //构建拼团订单
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
                    .validStartTime(payActivityEntity.getStartTime())  // 拼团开始时间
                    .validEndTime(payActivityEntity.getEndTime())      // 拼团结束时间
                    .notifyType("HTTP")    // 回调类型，默认为 HTTP
                    .notifyUrl("")         // 回调地址，可为空字符串
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
                .orderId(orderId)
                .deductionPrice(payDiscountEntity.getDeductionPrice())
                .tradeOrderStatusEnumVO(TradeOrderStatusEnumVO.CREATE)
                .build();
    }

    //根据活动ID查询活动
    @Override
    public GroupBuyActivityEntity queryGroupBuyActivityByActivityId(long activityId) {
        GroupBuyActivity groupBuyActivity = groupBuyActivityDao.queryGroupBuyActivityByActivityId(activityId);
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
}
