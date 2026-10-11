package com.ariy.retrolauncher.launch

/** Small process-local cache; elapsed time avoids wall-clock changes. Nulls are never cached. */
internal class TimedCache<K, V : Any>(
    private val maxEntries: Int,
    private val ttlMillis: Long,
    private val nowMillis: () -> Long = { System.nanoTime() / 1_000_000 }
) {
    private data class Entry<V>(val value: V, val createdAt: Long)
    private val entries = LinkedHashMap<K, Entry<V>>(16, 0.75f, true)

    init {
        require(maxEntries > 0 && ttlMillis > 0)
    }

    @Synchronized
    fun get(key: K): V? {
        val entry = entries[key] ?: return null
        if (nowMillis() - entry.createdAt >= ttlMillis) {
            entries.remove(key)
            return null
        }
        return entry.value
    }

    @Synchronized
    fun put(key: K, value: V) {
        entries[key] = Entry(value, nowMillis())
        while (entries.size > maxEntries) entries.remove(entries.keys.first())
    }

    @Synchronized
    fun clear() = entries.clear()
}