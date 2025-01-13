package zone.chenfeng.JMemPool;

import zone.chenfeng.JMemPool.impl.SimpleMemoryPool;
import net.jpountz.xxhash.XXHash32;
import net.jpountz.xxhash.XXHashFactory;

import java.nio.LongBuffer;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class MSet implements Set {
    LongBuffer pointers;
    IMemoryPool memoryPool;
    int size;
    int capacity;
    private final XXHash32 xxHash;
    private static final int DEFAULT_CAPACITY = 256;

    public MSet() {
        xxHash = XXHashFactory.fastestInstance().hash32();
        size = 0;
        capacity = DEFAULT_CAPACITY;
        pointers = LongBuffer.allocate(capacity);
        memoryPool = new SimpleMemoryPool();
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size==0;
    }

    @Override
    public boolean contains(Object o) {
        return false;
    }

    @Override
    public Iterator iterator() {
        return null;
    }

    @Override
    public Object[] toArray() {
        return new Object[0];
    }

    @Override
    public boolean add(Object o) {
        if(size<capacity) {
            //
        }
        return false;
    }

    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public boolean addAll( Collection c) {
        return false;
    }

    @Override
    public void clear() {

    }

    @Override
    public boolean removeAll(Collection c) {
        return false;
    }

    @Override
    public boolean retainAll(Collection c) {
        return false;
    }

    @Override
    public boolean containsAll( Collection c) {
        return false;
    }

    @Override
    public Object[] toArray( Object[] a) {
        return new Object[0];
    }
}
