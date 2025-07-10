package com.idormy.sms.forwarder.utils

import java.util.concurrent.ConcurrentHashMap
import com.idormy.sms.forwarder.utils.Log

/**
 * 消息重复检测工具类
 * 用于防止短信被多个广播接收器或服务重复处理
 */
object MessageDedupUtils {
    private val TAG: String = MessageDedupUtils::class.java.simpleName

    // 消息缓存，key为消息内容+发送方，value为处理时间
    private val messageCache = ConcurrentHashMap<String, Long>()
    
    // 缓存过期时间(毫秒)，10秒内的相同消息被视为重复
    private const val CACHE_TTL = 10_000L
    
    /**
     * 检查消息是否已处理过
     * @param content 消息内容
     * @param from 发送方
     * @return 如果消息已处理过返回true，否则返回false并将消息加入缓存
     */
    fun isDuplicate(content: String, from: String): Boolean {
        val now = System.currentTimeMillis()
        val key = generateKey(content, from)
        
        // 检查是否存在相同消息
        val lastProcessed = messageCache[key]
        if (lastProcessed != null && (now - lastProcessed) < CACHE_TTL) {
            Log.d(TAG, "找到重复消息: $key")
            return true
        }
        
        // 将消息加入缓存
        messageCache[key] = now
        
        // 清理过期缓存
        cleanupExpiredCache(now)
        
        return false
    }
    
    /**
     * 生成缓存键
     */
    private fun generateKey(content: String, from: String): String {
        // 简单组合内容和发送方作为键
        return "$from:$content"
    }
    
    /**
     * 清理过期缓存
     */
    private fun cleanupExpiredCache(now: Long) {
        // 避免每次都清理
        if (messageCache.size > 100) {
            val iterator = messageCache.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (now - entry.value > CACHE_TTL) {
                    iterator.remove()
                }
            }
        }
    }
} 