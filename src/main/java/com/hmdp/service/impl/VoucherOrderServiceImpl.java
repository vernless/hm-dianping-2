package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisWorker;
import com.hmdp.utils.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisWorker redisWorker;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    // 阻塞队列
    private BlockingQueue<VoucherOrder> oiderTasks = new ArrayBlockingQueue<>(1024*1024);
    private static final ExecutorService seckill_order_executor =
            Executors.newSingleThreadExecutor();

    @PostConstruct
    private void init(){
        seckill_order_executor.submit(new VoucherOrderHandler());
    }

    private class VoucherOrderHandler implements Runnable{
        @Override
        public void run() {
            while (true){
                try {
                    // 1 获取队列中的订单信息
                    VoucherOrder voucherOrder = oiderTasks.take();
                    // 2 创建订单
                    handleVoucherOrder(voucherOrder);
                } catch (InterruptedException e) {
                    log.error("处理订单异常", e);
                }
            }
        }
    }
    private void handleVoucherOrder(VoucherOrder voucherOrder) {
        // 获取用户
        Long userId = voucherOrder.getUserId();
        //创建锁对象
        //SimpleRedisLock lock = new SimpleRedisLock(stringRedisTemplate,"order:" + userId );
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        //获取锁
        boolean isLock = lock.tryLock();
        //判断是否获取锁成功
        if (!isLock) {
            //获取锁失败，返回错误或重试
            log.error("不允许重复下单");
            return;
        }
        try {
            // 获取代理对象（事务），避免未提交事务就释放锁
            proxy.createVoucherOrder(voucherOrder);
        } finally {
            //释放锁
            lock.unlock();
        }
    }

    private IVoucherOrderService proxy;

    @Override
    public Result seckillVoucher(Long voucherId) {
        // 获取用户
        Long userId = UserHolder.getUser().getId();
        // 1. 执行lua脚本
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                userId.toString());
        // 2. 判断结果是否为0
        int r = result.intValue();
        if (r != 0) {
            // 2.1 不为0，代表没有购买资格
            return Result.fail(r == 1 ? "库存不足" : "不能重复下单");
        }
        // 2.2 为0，有购买资格，把下单信息保存到阻塞队列
        Long orderId = redisWorker.nextId("order");
        // 保存到阻塞队列
        VoucherOrder voucherOrder = new VoucherOrder();
        // 2.3 订单id
        voucherOrder.setId(orderId);
        // 2.4 用户id
        voucherOrder.setUserId(userId);
        // 2.5 代金券id
        voucherOrder.setVoucherId(voucherId);
        // 2.6 放入阻塞队列
        oiderTasks.add(voucherOrder);
        // 3. 获取代理对象
        proxy = (IVoucherOrderService) AopContext.currentProxy();
        // 3. 返回订单id
        return Result.ok(orderId);
    }


    /*  原秒杀订单流程
        @Override
        public Result seckillVoucher(Long voucherId) {
            //1. 查询优惠券
            SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
            //2. 判断秒杀是否成功
            if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
                //尚未开始
                return Result.fail("秒杀尚未开始！");
            }
            //3. 判断秒杀是否结束
            if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
                //已经结束
                return Result.fail("秒杀已经结束！");
            }
            //4. 判断库存是否充足
            if (voucher.getStock() < 1) {
                //库存不足
                return Result.fail("库存不足！");
            }
            Long userId = UserHolder.getUser().getId();
            //创建锁对象
            //SimpleRedisLock lock = new SimpleRedisLock(stringRedisTemplate,"order:" + userId );
            RLock lock = redissonClient.getLock("lock:order:" + userId);
            //获取锁
            boolean isLock = lock.tryLock();
            //判断是否获取锁成功
            if (!isLock) {
                //获取锁失败，返回错误或重试
                return Result.fail("不允许重复下单");
            }
            try {
                // 获取代理对象（事务），避免未提交事务就释放锁
                IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
                return proxy.createVoucherOrder(voucherId, userId);
            } finally {
                //释放锁
                lock.unlock();
            }
        }
    */
    @Transactional
    public  void createVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();

        //5.1 查询订单
        int count = query().eq("user_id", userId).eq("voucher_id", voucherOrder.getVoucherId()).count();
        //5.2 判断是否存在
        if (count > 0) {
            //用户已购买
            log.error("用户已购买过");
            return;
        }
        //6. 减扣库存(使用乐观锁CAS判断库存,只要 库存>0 就可)
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherOrder.getVoucherId())
                .gt("stock",0)
                .update();
        if (!success) {
            //扣减失败
            log.error("库存不足！");
            return;
        }
        //7. 创建订单
        save(voucherOrder); 
    }
}
