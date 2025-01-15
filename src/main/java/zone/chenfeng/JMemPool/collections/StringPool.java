package zone.chenfeng.JMemPool.collections;

import zone.chenfeng.JMemPool.IMemoryPool;
import zone.chenfeng.JMemPool.impl.SimpleMemoryPool;

import java.nio.charset.StandardCharsets;

public class StringPool {
    IMemoryPool memoryPool;

    public StringPool() {
        memoryPool = new SimpleMemoryPool();
    }

    public StringPool(IMemoryPool memoryPool) {
        this.memoryPool = memoryPool;
    }

    public static StringPool newSimpleStringPool() {
        return new StringPool(new SimpleMemoryPool());
    }

    public long putString(String string) {
        return memoryPool.put(string.getBytes(StandardCharsets.UTF_8));
    }

    public long putString(long pointer,String string) {
        return memoryPool.put(pointer,string.getBytes(StandardCharsets.UTF_8));
    }

    public String getString(long pointer) {
        return new String(memoryPool.get(pointer), StandardCharsets.UTF_8);
    }

    public String free(long pointer) {
        String string = getString(pointer);
        memoryPool.free(pointer);
        return string;
    }
}
