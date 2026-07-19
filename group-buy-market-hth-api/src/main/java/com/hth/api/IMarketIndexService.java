package com.hth.api;

import com.hth.api.dto.GoodsMarketRequestDTO;
import com.hth.api.dto.GoodsMarketResponseDTO;
import com.hth.api.response.Response;

public interface IMarketIndexService {
    Response<GoodsMarketResponseDTO> queryGroupBuyMarketConfig(GoodsMarketRequestDTO goodsMarketRequestDTO);
}
