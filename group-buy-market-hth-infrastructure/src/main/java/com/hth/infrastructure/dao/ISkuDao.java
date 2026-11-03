package com.hth.infrastructure.dao;

import com.hth.infrastructure.dao.po.Sku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author hth
 * @description 商品查询
 * @create 2024-12-21 10:48
 */
@Mapper
public interface ISkuDao {

    Sku querySkuByGoodsId(String goodsId);

    List<Sku> querySkuByGoodsIds(@Param("goodsIdList") List<String> goodsIdList);
}
