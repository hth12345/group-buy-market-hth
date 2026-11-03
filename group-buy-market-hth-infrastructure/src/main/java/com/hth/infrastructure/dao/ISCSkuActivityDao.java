package com.hth.infrastructure.dao;

import com.hth.domain.activity.model.valobj.SCSkuActivityVO;
import com.hth.infrastructure.dao.po.SCSkuActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author hth
 * @description 渠道商品活动配置关联表Dao
 * @create 2025-01-01 09:30
 */
@Mapper
public interface ISCSkuActivityDao {

    SCSkuActivity querySCSkuActivityBySCGoodsId(SCSkuActivity scSkuActivity);

    List<SCSkuActivityVO> queryGoodByActivityIds(@Param("activityIds") List<Long> activityIds);
}
