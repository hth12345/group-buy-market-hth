package com.hth.test;

import com.hth.infrastructure.dao.IGroupBuyOrderDao;
import com.hth.infrastructure.dao.IGroupBuyOrderListDao;
import com.hth.infrastructure.dao.po.GroupBuyOrder;
import com.hth.infrastructure.dao.po.GroupBuyOrderList;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Random;

/**
 * 退单压测数据准备
 * 读取 CSV 文件，批量写入 group_buy_order 和 group_buy_order_list，为退单接口压测准备已支付订单数据
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class PrepareRefundTestData {

    @Resource
    private IGroupBuyOrderDao groupBuyOrderDao;

    @Resource
    private IGroupBuyOrderListDao groupBuyOrderListDao;

    private static final Long ACTIVITY_ID = 100123L;
    private static final String GOODS_ID = "9890001";
    private static final BigDecimal ORIGINAL_PRICE = new BigDecimal("99.00");
    private static final BigDecimal DEDUCTION_PRICE = new BigDecimal("10.00");
    private static final BigDecimal PAY_PRICE = new BigDecimal("89.00");
    private static final String NOTIFY_TYPE = "HTTP";
    private static final String NOTIFY_URL = "http://localhost:8091/test/notify";

    private final Random random = new Random();

    @Test
    public void test_prepareRefundData() {
        String csvPath = "d:/project/group-buy-market-hth/docs/dev-ops/test_data/refund_stress_test.csv";
        Date now = new Date();
        Date startTime = new Date(now.getTime() - 3600 * 1000L);
        Date endTime = new Date(now.getTime() + 23 * 3600 * 1000L);

        int successCount = 0;
        int skipCount = 0;
        int totalCount = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            String header = br.readLine(); // 跳过表头
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                totalCount++;

                String[] fields = line.split(",");
                String userId = fields[0].trim();
                String rawOutTradeNo = fields[1].trim();
                String source = fields[2].trim();
                String channel = fields[3].trim();

                // outTradeNo 截断处理：varchar(12)，"OUT_TRADE_NO_001"(16位) -> "TRADE_NO_001"(12位)
                String outTradeNo = rawOutTradeNo.length() > 12
                        ? rawOutTradeNo.substring(4)
                        : rawOutTradeNo;

                String teamId = generateTeamId();
                String orderId = generateOrderId();
                String bizId = ACTIVITY_ID + "_" + userId + "_1";

                try {
                    // 插入 group_buy_order（团队表）
                    GroupBuyOrder groupBuyOrder = GroupBuyOrder.builder()
                            .teamId(teamId)
                            .activityId(ACTIVITY_ID)
                            .source(source)
                            .channel(channel)
                            .originalPrice(ORIGINAL_PRICE)
                            .deductionPrice(DEDUCTION_PRICE)
                            .payPrice(PAY_PRICE)
                            .targetCount(1)
                            .completeCount(1)
                            .lockCount(1)
                            .status(1)
                            .validStartTime(startTime)
                            .validEndTime(endTime)
                            .notifyType(NOTIFY_TYPE)
                            .notifyUrl(NOTIFY_URL)
                            .build();
                    groupBuyOrderDao.insert(groupBuyOrder);

                    // 插入 group_buy_order_list（订单明细表）
                    GroupBuyOrderList orderList = GroupBuyOrderList.builder()
                            .userId(userId)
                            .teamId(teamId)
                            .orderId(orderId)
                            .activityId(ACTIVITY_ID)
                            .startTime(startTime)
                            .endTime(endTime)
                            .goodsId(GOODS_ID)
                            .source(source)
                            .channel(channel)
                            .originalPrice(ORIGINAL_PRICE)
                            .deductionPrice(DEDUCTION_PRICE)
                            .payPrice(PAY_PRICE)
                            .status(1)
                            .outTradeNo(outTradeNo)
                            .outTradeTime(now)
                            .bizId(bizId)
                            .build();
                    groupBuyOrderListDao.insert(orderList);

                    successCount++;
                    if (successCount % 10 == 0) {
                        log.info("已插入 {} 条数据，当前用户: {}", successCount, userId);
                    }
                } catch (Exception e) {
                    skipCount++;
                    log.warn("插入失败，跳过用户: {}，原因: {}", userId, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("读取 CSV 文件失败", e);
        }

        log.info("========== 数据准备完成 ==========");
        log.info("总计读取: {} 条", totalCount);
        log.info("成功插入: {} 条", successCount);
        log.info("跳过(重复): {} 条", skipCount);
    }

    /**
     * 生成 8 位随机数字 teamId
     */
    private String generateTeamId() {
        int num = random.nextInt(90000000) + 10000000;
        return String.valueOf(num);
    }

    /**
     * 生成 12 位随机数字 orderId
     */
    private String generateOrderId() {
        long num = (long) (random.nextDouble() * 900000000000L) + 100000000000L;
        return String.valueOf(num);
    }

}
