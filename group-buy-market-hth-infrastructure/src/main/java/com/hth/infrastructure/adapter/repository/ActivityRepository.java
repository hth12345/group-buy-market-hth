package com.hth.infrastructure.adapter.repository;

import com.hth.domain.activity.adapter.repository.IActivityRepository;
import com.hth.domain.activity.model.entity.ActivityInfoEntity;
import com.hth.domain.activity.model.entity.GoodsInfoEntity;
import com.hth.domain.activity.model.entity.UserGroupBuyOrderDetailEntity;
import com.hth.domain.activity.model.valobj.*;
import com.hth.infrastructure.dao.*;
import com.hth.infrastructure.dao.po.*;
import com.hth.infrastructure.dcc.DCCService;
import com.hth.infrastructure.redis.IRedisService;
import lombok.Builder;
import org.redisson.api.RBitSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Builder
@Repository
public class ActivityRepository extends AbstractRepository implements IActivityRepository {
    private static final Logger log = LoggerFactory.getLogger(ActivityRepository.class);

    @Resource
    private IGroupBuyActivityDao groupBuyActivityDao;
    @Resource
    private IGroupBuyDiscountDao groupBuyDiscountDao;
    @Resource
    private ISkuDao skuDao;
    @Resource
    private ISCSkuActivityDao scSkuActivityDao;
    @Resource
    private IRedisService redisService;
    @Resource
    private DCCService dccService;
    @Resource
    private IGroupBuyOrderDao groupBuyOrderDao;
    @Resource
    private IGroupBuyOrderListDao groupBuyOrderListDao;

    public GroupBuyActivityDiscountVO queryGroupBuyActivityDiscountVO(long activityId) {
        // 根据活动id查询配置中最新的1个有效的活动
        String groupBuyActivityCacheKey = GroupBuyActivity.cacheRedisKey(activityId);
        GroupBuyActivity groupBuyActivityRes = getFromCacheOrDb(groupBuyActivityCacheKey, ()->groupBuyActivityDao.queryValidGroupBuyActivityId(activityId),20000L);

        // 根据折扣id获取对应折扣
        String discountId = groupBuyActivityRes.getDiscountId();
        String groupBuyDiscountCacheKey = GroupBuyDiscount.cacheRedisKey(discountId);
        GroupBuyDiscount groupBuyDiscountRes = getFromCacheOrDb(groupBuyDiscountCacheKey, () -> groupBuyDiscountDao.queryGroupBuyActivityDiscountByDiscountId(discountId), 20000L);

        GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountName(groupBuyDiscountRes.getDiscountName())
                .discountDesc(groupBuyDiscountRes.getDiscountDesc())
                .discountType(DiscountTypeEnum.get(groupBuyDiscountRes.getDiscountType()))
                .marketPlan(groupBuyDiscountRes.getMarketPlan())
                .marketExpr(groupBuyDiscountRes.getMarketExpr())
                .tagId(groupBuyDiscountRes.getTagId())
                .build();

        return GroupBuyActivityDiscountVO.builder()
                .activityId(groupBuyActivityRes.getActivityId())
                .activityName(groupBuyActivityRes.getActivityName())
                .groupBuyDiscount(groupBuyDiscount)
                .groupType(groupBuyActivityRes.getGroupType())
                .takeLimitCount(groupBuyActivityRes.getTakeLimitCount())
                .target(groupBuyActivityRes.getTarget())
                .validTime(groupBuyActivityRes.getValidTime())
                .status(groupBuyActivityRes.getStatus())
                .startTime(groupBuyActivityRes.getStartTime())
                .endTime(groupBuyActivityRes.getEndTime())
                .tagId(groupBuyActivityRes.getTagId())
                .tagScope(groupBuyActivityRes.getTagScope())
                .build();
    }

    @Override
    public SkuVO querySkuByGoodsId(String goodsId) {
        Sku sku = skuDao.querySkuByGoodsId(goodsId);
        if(sku == null) {
            return null;
        }
        return SkuVO.builder()
                .goodsId(sku.getGoodsId())
                .goodsName(sku.getGoodsName())
                .originalPrice(sku.getOriginalPrice())
                .build();
    }

    @Override
    public SCSkuActivityVO querySCSkuActivityBySCGoodsId(String source, String channel, String goodsId) {
        SCSkuActivity scSkuActivityReq = new SCSkuActivity();
        scSkuActivityReq.setSource(source);
        scSkuActivityReq.setChannel(channel);
        scSkuActivityReq.setGoodsId(goodsId);
        SCSkuActivity scSkuActivity = scSkuActivityDao.querySCSkuActivityBySCGoodsId(scSkuActivityReq);
        if(scSkuActivity == null) {
            return null;
        }

        return SCSkuActivityVO.builder()
                .source(scSkuActivity.getSource())
                .channel(scSkuActivity.getChannel())
                .goodsId(scSkuActivity.getGoodsId())
                .activityId(scSkuActivity.getActivityId())
                .build();
    }

    @Override
    public boolean isTagCrowdRange(String tagId, String userId) {
        RBitSet bitSet = redisService.getBitSet(tagId);
        if(!bitSet.isExists()){
            return true;
        }
        // 判断用户是否存在人群中
        return bitSet.get(redisService.getIndexFromUserId(userId));
    }

    @Override
    public boolean downgradeSwitch() {
        return dccService.isDowngradeSwitch();
    }

    @Override
    public boolean cutRange(String userId) {
        return dccService.isCutRange(userId);
    }

    @Override
    public boolean whiteList(String userId) {
        return dccService.isWhiteList(userId);
    }

    @Override
    public List<UserGroupBuyOrderDetailEntity> queryInProgressUserGroupBuyOrderDetailListByOwner(Long activityId, String userId, int ownerCount) {
        // 1. 根据用户ID、活动ID，查询用户参与的拼团队伍
        GroupBuyOrderList groupBuyOrderListReq = new GroupBuyOrderList();
        groupBuyOrderListReq.setActivityId(activityId);
        groupBuyOrderListReq.setUserId(userId);
        groupBuyOrderListReq.setCount(ownerCount);
        List<GroupBuyOrderList> groupBuyOrderLists = groupBuyOrderListDao.queryInProgressUserGroupBuyOrderDetailListByUserId(groupBuyOrderListReq);
        if (null == groupBuyOrderLists || groupBuyOrderLists.isEmpty()) return null;

        // 2. 过滤队伍获取 TeamId
        Set<String> teamIds = groupBuyOrderLists.stream()
                .map(GroupBuyOrderList::getTeamId)
                .filter(teamId -> teamId != null && !teamId.isEmpty()) // 过滤非空和非空字符串
                .collect(Collectors.toSet());

        // 3. 查询队伍明细，组装Map结构
        List<GroupBuyOrder> groupBuyOrders = groupBuyOrderDao.queryGroupBuyProgressByTeamIds(teamIds);
        if (null == groupBuyOrders || groupBuyOrders.isEmpty()) return null;

        Map<String, GroupBuyOrder> groupBuyOrderMap = groupBuyOrders.stream()
                .collect(Collectors.toMap(GroupBuyOrder::getTeamId, order -> order));

        // 4. 转换数据
        List<UserGroupBuyOrderDetailEntity> userGroupBuyOrderDetailEntities = new ArrayList<>();
        for (GroupBuyOrderList groupBuyOrderList : groupBuyOrderLists) {
            String teamId = groupBuyOrderList.getTeamId();
            GroupBuyOrder groupBuyOrder = groupBuyOrderMap.get(teamId);
            if (null == groupBuyOrder) continue;

            UserGroupBuyOrderDetailEntity userGroupBuyOrderDetailEntity = UserGroupBuyOrderDetailEntity.builder()
                    .userId(groupBuyOrderList.getUserId())
                    .teamId(groupBuyOrder.getTeamId())
                    .activityId(groupBuyOrder.getActivityId())
                    .targetCount(groupBuyOrder.getTargetCount())
                    .completeCount(groupBuyOrder.getCompleteCount())
                    .lockCount(groupBuyOrder.getLockCount())
                    .validStartTime(groupBuyOrder.getValidStartTime())
                    .validEndTime(groupBuyOrder.getValidEndTime())
                    .outTradeNo(groupBuyOrderList.getOutTradeNo())
                    .build();

            userGroupBuyOrderDetailEntities.add(userGroupBuyOrderDetailEntity);
        }

        return userGroupBuyOrderDetailEntities;
    }

    @Override
    public List<UserGroupBuyOrderDetailEntity> queryInProgressUserGroupBuyOrderDetailListByRandom(Long activityId, String userId, int randomCount) {
        // 1. 根据用户ID、活动ID，查询用户参与的拼团队伍
        GroupBuyOrderList groupBuyOrderListReq = new GroupBuyOrderList();
        groupBuyOrderListReq.setActivityId(activityId);
        groupBuyOrderListReq.setUserId(userId);
        groupBuyOrderListReq.setCount(randomCount * 2); // 查询2倍的量，之后其中 randomCount 数量
        List<GroupBuyOrderList> groupBuyOrderLists = groupBuyOrderListDao.queryInProgressUserGroupBuyOrderDetailListByRandom(groupBuyOrderListReq);
        if (null == groupBuyOrderLists || groupBuyOrderLists.isEmpty()) return null;

        // 判断总量是否大于 randomCount
        if (groupBuyOrderLists.size() > randomCount) {
            // 随机打乱列表
            Collections.shuffle(groupBuyOrderLists);
            // 获取前 randomCount 个元素
            groupBuyOrderLists = groupBuyOrderLists.subList(0, randomCount);
        }

        // 2. 过滤队伍获取 TeamId
        Set<String> teamIds = groupBuyOrderLists.stream()
                .map(GroupBuyOrderList::getTeamId)
                .filter(teamId -> teamId != null && !teamId.isEmpty()) // 过滤非空和非空字符串
                .collect(Collectors.toSet());

        // 3. 查询队伍明细，组装Map结构
        List<GroupBuyOrder> groupBuyOrders = groupBuyOrderDao.queryGroupBuyProgressByTeamIds(teamIds);
        if (null == groupBuyOrders || groupBuyOrders.isEmpty()) return null;

        Map<String, GroupBuyOrder> groupBuyOrderMap = groupBuyOrders.stream()
                .collect(Collectors.toMap(GroupBuyOrder::getTeamId, order -> order));

        // 4. 转换数据
        List<UserGroupBuyOrderDetailEntity> userGroupBuyOrderDetailEntities = new ArrayList<>();
        for (GroupBuyOrderList groupBuyOrderList : groupBuyOrderLists) {
            String teamId = groupBuyOrderList.getTeamId();
            GroupBuyOrder groupBuyOrder = groupBuyOrderMap.get(teamId);
            if (null == groupBuyOrder) continue;

            UserGroupBuyOrderDetailEntity userGroupBuyOrderDetailEntity = UserGroupBuyOrderDetailEntity.builder()
                    .userId(groupBuyOrderList.getUserId())
                    .teamId(groupBuyOrder.getTeamId())
                    .activityId(groupBuyOrder.getActivityId())
                    .targetCount(groupBuyOrder.getTargetCount())
                    .completeCount(groupBuyOrder.getCompleteCount())
                    .lockCount(groupBuyOrder.getLockCount())
                    .validStartTime(groupBuyOrder.getValidStartTime())
                    .validEndTime(groupBuyOrder.getValidEndTime())
                    .build();

            userGroupBuyOrderDetailEntities.add(userGroupBuyOrderDetailEntity);
        }

        return userGroupBuyOrderDetailEntities;
    }

    @Override
    @Transactional(timeout = 500)
    public TeamStatisticVO queryTeamStatisticByActivityId(Long activityId) {
        // 1. 根据活动ID查询拼团队伍
        List<GroupBuyOrderList> groupBuyOrderLists = groupBuyOrderListDao.queryInProgressUserGroupBuyOrderDetailListByActivityId(activityId);

        if (null == groupBuyOrderLists || groupBuyOrderLists.isEmpty()) {
            return new TeamStatisticVO(0, 0, 0);
        }

        // 2. 过滤队伍获取 TeamId
        Set<String> teamIds = groupBuyOrderLists.stream()
                .map(GroupBuyOrderList::getTeamId)
                .filter(teamId -> teamId != null && !teamId.isEmpty()) // 过滤非空和非空字符串
                .collect(Collectors.toSet());

        // 3. 统计数据
        Integer allTeamCount = groupBuyOrderDao.queryAllTeamCount(teamIds);
        Integer allTeamCompleteCount = groupBuyOrderDao.queryAllTeamCompleteCount(teamIds);
        Integer allTeamUserCount = groupBuyOrderDao.queryAllUserCount(teamIds);

        // 4. 构建对象
        return TeamStatisticVO.builder()
                .allTeamCount(allTeamCount)
                .allTeamCompleteCount(allTeamCompleteCount)
                .allTeamUserCount(allTeamUserCount)
                .build();
    }

    // 查询所有活动
    @Override
    public List<ActivityInfoEntity> queryAllActivities() {
        // 生成缓存键
        String cacheKey = ActivityInfoEntity.cacheRedisKey();
        log.info("开始查询所有活动，缓存键: {}", cacheKey);

        // 使用父类的缓存方法
        return getFromCacheOrDb(cacheKey, () -> {
            log.info("缓存未命中，从数据库查询所有活动");

            List<GroupBuyActivity> activityList = groupBuyActivityDao.queryAllActivities();
            log.info("查询到的活动数量: {}", activityList != null ? activityList.size() : 0);

            List<ActivityInfoEntity> activityInfoEntities = new ArrayList<>();
            for (GroupBuyActivity activity : activityList) {
                activityInfoEntities.add(ActivityInfoEntity.builder()
                        .activityId(activity.getActivityId())
                        .activityName(activity.getActivityName())
                        .discountId(activity.getDiscountId())
                        .groupType(activity.getGroupType())
                        .takeLimitCount(activity.getTakeLimitCount())
                        .target(activity.getTarget())
                        .validTime(activity.getValidTime())
                        .status(activity.getStatus())
                        .tagId(activity.getTagId())
                        .tagScope(activity.getTagScope())
                        .startTime(activity.getStartTime())
                        .endTime(activity.getEndTime())
                        .build());
            }

            log.info("最终组装的活动信息数量: {}", activityInfoEntities.size());
            return activityInfoEntities;
        }, 20000L);  // 缓存20秒
    }

    @Override
    @Transactional(timeout = 500)
    public List<GoodsInfoEntity> queryAllGoods(List<Long> activityIds) {
        log.info("开始查询所有商品，活动ID列表: {}", activityIds);

        // 生成缓存键
        String cacheKey = GoodsInfoEntity.cacheRedisKey(activityIds);
        log.info("缓存键: {}", cacheKey);

        // 使用父类的缓存方法
        return getFromCacheOrDb(cacheKey, () -> {
            log.info("缓存未命中，从数据库查询");

            List<SCSkuActivityVO> scSkuActivityList  = scSkuActivityDao.queryGoodByActivityIds(activityIds);
            log.info("查询到的活动商品关联数据: {}", scSkuActivityList != null ? scSkuActivityList.size() : 0);

            if (null == scSkuActivityList || scSkuActivityList.isEmpty())
                return Collections.emptyList();

            // 过滤 null 元素并提取 goodsId
            List<String> goodsIdList = scSkuActivityList.stream()
                    .filter(vo -> {
                        if (vo == null) {
                            log.warn("发现 null 的 SCSkuActivityVO 对象");
                            return false;
                        }
                        if (vo.getGoodsId() == null) {
                            log.warn("发现 goodsId 为 null 的 SCSkuActivityVO: {}", vo);
                            return false;
                        }
                        return true;
                    })
                    .map(SCSkuActivityVO::getGoodsId)
                    .collect(Collectors.toList());

            log.info("过滤后的商品ID列表: {}", goodsIdList);

            if (goodsIdList.isEmpty())
                return Collections.emptyList();

            List<Sku> skuList = skuDao.querySkuByGoodsIds(goodsIdList);
            log.info("查询到的SKU数据: {}", skuList != null ? skuList.size() : 0);

            if (null == skuList || skuList.isEmpty())
                return Collections.emptyList();

            Map<String, Sku> skuMap = skuList.stream()
                    .filter(sku -> {
                        if (sku == null) {
                            log.warn("发现 null 的 Sku 对象");
                            return false;
                        }
                        if (sku.getGoodsId() == null) {
                            log.warn("发现 goodsId 为 null 的 Sku: {}", sku);
                            return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toMap(Sku::getGoodsId, sku -> sku, (v1, v2) -> v1));

            log.info("构建的SKU Map大小: {}", skuMap.size());

            List<GoodsInfoEntity> goodsInfoEntities = new ArrayList<>();
            for (SCSkuActivityVO scSkuActivity : scSkuActivityList) {
                if (scSkuActivity == null || scSkuActivity.getGoodsId() == null) continue;

                Sku sku = skuMap.get(scSkuActivity.getGoodsId());
                if (sku == null) continue;

                goodsInfoEntities.add(GoodsInfoEntity.builder()
                        .activityId(scSkuActivity.getActivityId())
                        .goodsId(sku.getGoodsId())
                        .goodsName(sku.getGoodsName())
                        .originalPrice(sku.getOriginalPrice())
                        .build());
            }

            log.info("最终组装的商品信息数量: {}", goodsInfoEntities.size());
            return goodsInfoEntities;
        }, 20000L);  // 缓存20秒
    }
}
