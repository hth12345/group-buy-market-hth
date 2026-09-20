package com.hth.api;

import com.hth.api.dto.LockMarketPayOrderRequestDTO;
import com.hth.api.dto.LockMarketPayOrderResponseDTO;
import com.hth.api.dto.SettlementMarketPayOrderRequestDTO;
import com.hth.api.dto.SettlementMarketPayOrderResponseDTO;
import com.hth.api.response.Response;

public interface IMarketTradeService {

    //营销锁单
    Response<LockMarketPayOrderResponseDTO> lockMarketPayOrder(LockMarketPayOrderRequestDTO lockMarketPayOrderRequestDTO);

    //营销结算
    Response<SettlementMarketPayOrderResponseDTO> settlementMarketPayOrder(SettlementMarketPayOrderRequestDTO requestDTO);
}
