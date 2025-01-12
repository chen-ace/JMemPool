package JMemPool;

import net.jpountz.xxhash.XXHash64;
import net.jpountz.xxhash.XXHashFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

record Index(int offset, int length) {
}

public class XHashMap<K, V> implements Map<K, V> {

    ByteBuffer byteBuffer;
    Index[] indexes;
    int size;
    private final XXHash64 xxHash;

    public XHashMap() {
        this(16);
    }

    public XHashMap(int capacity) {
        this.xxHash = XXHashFactory.fastestInstance().hash64();
        indexes = new Index[capacity];
        byteBuffer = ByteBuffer.allocate(capacity * 1024 * 1024);
        size = 0;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        byte[] keyBytes = key.toString().getBytes(StandardCharsets.UTF_8);
        long hash = xxHash.hash(keyBytes, 0, keyBytes.length, 0);
        return containsKeyHash(hash);
    }

    public boolean containsKeyHash(long keyHash) {
        int index = (int) (keyHash % indexes.length);
        Index i = indexes[index];
        if (i == null) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public V get(Object key) {
        byte[] keyBytes = key.toString().getBytes(StandardCharsets.UTF_8);
        long hash = xxHash.hash(keyBytes, 0, keyBytes.length, 0);
        if (containsKeyHash(hash)) {
            Index index = indexes[(int) (hash % indexes.length)];
            // 保存当前position
            int originalPosition = byteBuffer.position();
            try {
                byteBuffer.position(index.offset());
                byte[] valueBytes = new byte[index.length()];
                byteBuffer.get(valueBytes);
                String s = new String(valueBytes, StandardCharsets.UTF_8);
                return (V) s;
            } finally {
                byteBuffer.position(originalPosition);
            }
        } else {
            return null;
        }
    }

    @Override
    public V put(K key, V value) {
        byte[] keyBytes = key.toString().getBytes(StandardCharsets.UTF_8);
        long hash = xxHash.hash(keyBytes, 0, keyBytes.length, 0);
        if (containsKeyHash(hash)) {
            //TODO update value
            return null;
        } else {
            long offset = byteBuffer.position();
            byte[] valueBytes = value.toString().getBytes(StandardCharsets.UTF_8);
            long length = valueBytes.length;
            byteBuffer.put(valueBytes);
            Index index = new Index((int) offset, (int) length);
            indexes[(int) (hash % indexes.length)] = index;
            size++;
            return value;
        }
    }

    @Override
    public boolean containsValue(Object value) {
        return false;
    }

    @Override
    public V remove(Object key) {
        return null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {

    }

    @Override
    public void clear() {

    }

    @Override
    public Set<K> keySet() {
        return Set.of();
    }

    @Override
    public Collection<V> values() {
        return List.of();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return Set.of();
    }
}
