package SimpleHashMap;

import JMemPool.impl.SimpleMemoryPool;

import java.util.HashMap;

public class SimpleHashMap<K, V> {
    private static final int DEFAULT_INITIAL_CAPACITY = 16;
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;

    private Entry<K, V>[] buckets;
    private int size;
    private SimpleMemoryPool memoryPool;

    private static class Entry<K,V>{
        K key;
        V value;
        long nextPointer;

        Entry(K key, V value) {
            this.key = key;
            this.value = value;
            this.nextPointer = -1;
        }
    }

    public SimpleHashMap() {
        buckets = new Entry[DEFAULT_INITIAL_CAPACITY];
        size = 0;
        memoryPool = new SimpleMemoryPool();
    }

    private int hash(K key) {
        return key==null ? 0 : Math.abs(key.hashCode() % buckets.length);
    }

    // 序列化Entry对象
    private byte[] serializeEntry(Entry<K, V> entry) {
        // 这里需要实现序列化逻辑，将Entry对象转换为byte数组
        // 具体实现取决于K和V的类型
        // 这里仅作示例，实际使用时需要根据具体类型实现完整的序列化逻辑
        byte[] data = new byte[256]; // 假设每个Entry固定使用256字节
        // 实现序列化逻辑...
        return data;
    }

    // 反序列化Entry对象
    private Entry<K, V> deserializeEntry(byte[] data) {
        // 这里需要实现反序列化逻辑，将byte数组转换回Entry对象
        // 具体实现取决于K和V的类型
        // 这里仅作示例，实际使用时需要根据具体类型实现完整的反序列化逻辑
        Entry<K, V> entry = new Entry<>(null, null);
        // 实现反序列化逻辑...
        return entry;
    }

    public V put(K key, V value) {
        if (size >= buckets.length * DEFAULT_LOAD_FACTOR) {
            resize();
        }
        int index = hash(key);
        Entry<K,V> newEntry = new Entry<>(key, value);

        // 如果桶为空，直接插入
        if (buckets[index] == null) {
            memoryPool.put(serializeEntry(newEntry));
            buckets[index] = newEntry;
            size++;
            return null;
        }

        // 查找key是否已存在
        Entry<K, V> current = buckets[index];
        while (current != null) {
            if (key == null ? current.key == null : key.equals(current.key)) {
                V oldValue = current.value;
                current.value = value;
                memoryPool.put(current.nextPointer,serializeEntry(current));
                return oldValue;
            }
            if(current.nextPointer == -1){
                break;
            }
            current = deserializeEntry(memoryPool.get(current.nextPointer));
        }

        // 在链表尾部插入新节点
        long pointer = memoryPool.put(serializeEntry(newEntry));
        current.nextPointer = pointer;
        memoryPool.put(current.nextPointer,serializeEntry(current));
        size++;
        return null;
    }


    public V get(K key) {
        int index = hash(key);
        Entry<K, V> entry = buckets[index];
        while (entry != null) {
            if (key == null ? entry.key == null : key.equals(entry.key)) {
                return entry.value;
            }
            if(entry.nextPointer == -1){
                break;
            }
            entry = deserializeEntry(memoryPool.get(entry.nextPointer));
        }
        return null;
    }

    public V remove(K key) {
        int index = hash(key);
        Entry<K, V> entry = buckets[index];
        Entry<K, V> prev = null;

        while (entry != null) {
            if (key == null ? entry.key == null : key.equals(entry.key)) {
                if (prev == null) {
                    buckets[index] = deserializeEntry(memoryPool.get(entry.nextPointer));
                } else {
                    prev.nextPointer = entry.nextPointer;
                    memoryPool.put(serializeEntry(prev));
                }
                size--;
                return entry.value;
            }
            prev = entry;
            if(entry.nextPointer == -1){
                break;
            }
            entry = deserializeEntry(memoryPool.get(entry.nextPointer));
        }
        return null;
    }

    public void resize() {
        Entry<K, V>[] oldBuckets = buckets;
        buckets = new Entry[oldBuckets.length * 2];
        size = 0;

        for (Entry<K, V> entry : oldBuckets) {
            while (entry != null) {
                put(entry.key, entry.value);
                if(entry.nextPointer == -1){
                    break;
                }
                entry = deserializeEntry(memoryPool.get(entry.nextPointer));
            }
        }
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public static void main(String[] args) {
        SimpleHashMap sap = new SimpleHashMap();
        sap.put(1, "one");
        sap.put("two", "two");
        System.out.println(sap.get("two"));
    }
}
