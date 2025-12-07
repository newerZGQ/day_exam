package com.gorden.dayexam.repository

import com.gorden.dayexam.repository.model.PaperDetail
import java.util.concurrent.ConcurrentHashMap

/**
 * 单例类，用于缓存本次启动后打开过的所有 PaperDetail 对象
 * 使用 ConcurrentHashMap 保证线程安全
 */
object PaperDetailCache {

    // 使用 ConcurrentHashMap 存储 paperId -> PaperDetail 的映射
    private val cache = ConcurrentHashMap<Int, PaperDetail>()

    /**
     * 根据 paperId 获取缓存的 PaperDetail
     * @param paperId 试卷ID
     * @return PaperDetail 对象，如果不存在则返回 null
     */
    fun get(paperId: Int): PaperDetail? {
        return cache[paperId]
    }

    /**
     * 缓存 PaperDetail 对象
     * @param paperId 试卷ID
     * @param paperDetail PaperDetail 对象
     */
    fun put(paperId: Int, paperDetail: PaperDetail) {
        cache[paperId] = paperDetail
    }

    /**
     * 检查是否已缓存指定的 PaperDetail
     * @param paperId 试卷ID
     * @return true 如果已缓存，否则返回 false
     */
    fun contains(paperId: Int): Boolean {
        return cache.containsKey(paperId)
    }

    /**
     * 移除指定的缓存
     * @param paperId 试卷ID
     * @return 被移除的 PaperDetail，如果不存在则返回 null
     */
    fun remove(paperId: Int): PaperDetail? {
        return cache.remove(paperId)
    }

    /**
     * 清空所有缓存
     * 通常在应用退出或需要释放内存时调用
     */
    fun clear() {
        cache.clear()
    }

    /**
     * 获取当前缓存的数量
     * @return 缓存的 PaperDetail 数量
     */
    fun size(): Int {
        return cache.size
    }

    /**
     * 获取所有已缓存的 paperId
     * @return paperId 集合
     */
    fun getAllCachedPaperIds(): Set<Int> {
        return cache.keys.toSet()
    }
}
