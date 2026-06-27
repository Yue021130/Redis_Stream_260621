package com.example.redisstream.controller;

import com.example.redisstream.dto.ApiResponse;
import com.example.redisstream.service.RedisHashUseCasesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 演示 Redis Hash 经典使用场景 API
 */
@RestController
@RequestMapping("/api/redis-usecases/hash")
public class RedisHashUseCasesController {

    @Autowired
    private RedisHashUseCasesService redisHashUseCasesService;

    // --- 1. 对象缓存场景 ---
    
    @PostMapping("/user/{userId}")
    public ApiResponse<String> saveUser(@PathVariable String userId, @RequestBody Map<String, String> userInfo) {
        redisHashUseCasesService.saveUserObject(userId, userInfo);
        return ApiResponse.success("保存用户对象成功", null);
    }

    @PostMapping("/user/{userId}/points")
    public ApiResponse<Long> addPoints(@PathVariable String userId, @RequestParam long delta) {
        long newPoints = redisHashUseCasesService.updateUserPoint(userId, delta);
        return ApiResponse.success("积分更新成功", newPoints);
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<Map<Object, Object>> getUser(@PathVariable String userId) {
        Map<Object, Object> user = redisHashUseCasesService.getUserObject(userId);
        return ApiResponse.success("获取用户信息成功", user);
    }

    // --- 2. 购物车场景 ---

    @PostMapping("/cart/{userId}/add")
    public ApiResponse<String> addToCart(@PathVariable String userId, @RequestParam String productId, @RequestParam int quantity) {
        redisHashUseCasesService.addToCart(userId, productId, quantity);
        return ApiResponse.success("添加购物车成功", null);
    }

    @DeleteMapping("/cart/{userId}/remove")
    public ApiResponse<String> removeFromCart(@PathVariable String userId, @RequestParam String productId) {
        redisHashUseCasesService.removeFromCart(userId, productId);
        return ApiResponse.success("移除购物车商品成功", null);
    }

    @GetMapping("/cart/{userId}")
    public ApiResponse<Map<Object, Object>> getCart(@PathVariable String userId) {
        Map<Object, Object> cart = redisHashUseCasesService.getCartItems(userId);
        return ApiResponse.success("获取购物车信息成功", cart);
    }
}
