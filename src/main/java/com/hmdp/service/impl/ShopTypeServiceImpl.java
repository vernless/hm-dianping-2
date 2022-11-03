package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryList() {
        String key = RedisConstants.CACHE_SHOP_TYPE_LIST;
        String typekey = RedisConstants.CACHE_SHOP_TYPE;
        //1. 从redis缓存中查找shopTypeList，并以此为key
        List<String> typeList = stringRedisTemplate.opsForList().range(key,0,-1);
        List<ShopType> shopTypes = new ArrayList<>();
        //2. 判断是否存在
        if (!typeList.isEmpty()) {
            //3. 存在则把redis中的元素加入List后返回
            for(String listkey : typeList){
                String shopTypeJson = stringRedisTemplate.opsForValue().get(typekey + listkey);
                shopTypes.add(JSONUtil.toBean(shopTypeJson,ShopType.class));
            }
            return Result.ok(shopTypes);
        }
        //4. 不存在，则直接查找数据库
        shopTypes =  query().orderByAsc("sort").list();
        //5. 把list写入redis
        for(ShopType shopType : shopTypes){
            stringRedisTemplate.opsForList().rightPush(key,shopType.getSort().toString());
            stringRedisTemplate.opsForValue().set(typekey + shopType.getSort(), JSONUtil.toJsonStr(shopType),RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        }
        stringRedisTemplate.expire(key,RedisConstants.CACHE_SHOP_TYPE_TTL, TimeUnit.MINUTES);
        //6. 返回
        return Result.ok(shopTypes);
    }
}
