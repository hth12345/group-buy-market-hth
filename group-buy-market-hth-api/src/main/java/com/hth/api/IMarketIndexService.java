package com.hth.api;

import com.hth.api.dto.AllActivitiesRequestDTO;
import com.hth.api.dto.AllActivitiesResponseDTO;
import com.hth.api.dto.GoodsMarketRequestDTO;
import com.hth.api.dto.GoodsMarketResponseDTO;
import com.hth.api.response.Response;

import java.util.List;

public interface IMarketIndexService {
    Response<GoodsMarketResponseDTO> queryGroupBuyMarketConfig(GoodsMarketRequestDTO goodsMarketRequestDTO);
    Response<List<AllActivitiesResponseDTO>> queryAllActivities(AllActivitiesRequestDTO allActivitiesRequestDTO);
}
