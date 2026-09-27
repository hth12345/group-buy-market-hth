package com.hth.domain.trade.service.refund;

import cn.bugstack.wrench.design.framework.link.model2.chain.BusinessLinkedList;
import com.hth.domain.activity.model.entity.UserGroupBuyOrderDetailEntity;
import com.hth.domain.trade.adapter.repository.ITradeRepository;
import com.hth.domain.trade.model.entity.*;
import com.hth.domain.trade.model.valobj.RefundTypeEnumVO;
import com.hth.domain.trade.model.valobj.TeamRefundSuccess;
import com.hth.domain.trade.model.valobj.TradeOrderStatusEnumVO;
import com.hth.domain.trade.service.ITradeRefundOrderService;
import com.hth.domain.trade.service.refund.business.IRefundOrderStrategy;
import com.hth.domain.trade.service.refund.factory.TradeRefundRuleFilterFactory;
import com.hth.types.enums.GroupBuyOrderEnumVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
@Slf4j
@Service
public class TradeRefundOrderService implements ITradeRefundOrderService {
    private final ITradeRepository repository;
    private final Map<String, IRefundOrderStrategy> refundOrderStrategyMap;

    public TradeRefundOrderService(ITradeRepository repository, Map<String, IRefundOrderStrategy> refundOrderStrategyMap) {
        this.repository = repository;
        this.refundOrderStrategyMap = refundOrderStrategyMap;
    }
    @Resource
    private BusinessLinkedList<TradeRefundCommandEntity,TradeRefundRuleFilterFactory.DynamicContext,TradeRefundBehaviorEntity> tradeRefundRuleFilter;
    @Override
    public TradeRefundBehaviorEntity refundOrder(TradeRefundCommandEntity tradeRefundCommandEntity) throws Exception {
        log.info("逆向流程，退单操作 userId:{} outTradeNo:{}", tradeRefundCommandEntity.getUserId(), tradeRefundCommandEntity.getOutTradeNo());
        return tradeRefundRuleFilter.apply(tradeRefundCommandEntity, new TradeRefundRuleFilterFactory.DynamicContext());
    }

    @Override
    public void restoreTeamLockStock(TeamRefundSuccess teamRefundSuccess) throws Exception {
        log.info("逆向流程，恢复锁单量 userId:{} activityId:{} teamId:{}", teamRefundSuccess.getUserId(), teamRefundSuccess.getActivityId(), teamRefundSuccess.getTeamId());
        String type = teamRefundSuccess.getType();

        // 根据枚举值获取对应的退单类型
        RefundTypeEnumVO refundTypeEnumVO = RefundTypeEnumVO.getRefundTypeEnumVOByCode(type);
        IRefundOrderStrategy refundOrderStrategy = refundOrderStrategyMap.get(refundTypeEnumVO.getStrategy());

        // 逆向库存操作，恢复锁单量
        refundOrderStrategy.reverseStock(teamRefundSuccess);
    }

    @Override
    public List<UserGroupBuyOrderDetailEntity> queryTimeoutUnpaidOrderList() {
        log.info("扫描数据，超时组队未支付订单");
        return repository.queryTimeoutUnpaidOrderList();
    }
}
