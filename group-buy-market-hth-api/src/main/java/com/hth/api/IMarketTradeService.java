package com.hth.api;

import com.hth.api.dto.LockMarketPayOrderRequestDTO;
import com.hth.api.dto.LockMarketPayOrderResponseDTO;
import com.hth.api.dto.SettlementMarketPayOrderRequestDTO;
import com.hth.api.dto.SettlementMarketPayOrderResponseDTO;
import com.hth.api.response.Response;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 营销交易服务接口
 * @create 2025-01-11 13:49
 */
public interface IMarketTradeService {

    //营销锁单
    Response<LockMarketPayOrderResponseDTO> lockMarketPayOrder(LockMarketPayOrderRequestDTO lockMarketPayOrderRequestDTO);

    //营销结算
    Response<SettlementMarketPayOrderResponseDTO> settlementMarketPayOrder(SettlementMarketPayOrderRequestDTO requestDTO);
}
