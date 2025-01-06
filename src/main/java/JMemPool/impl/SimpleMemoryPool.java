package JMemPool.impl;

import JMemPool.IMemoryPool;

/**
 * Simple memory pool implementation.<br>
 * {@code SimpleMemoryPool} is a simple implementation of the {@link IMemoryPool} interface that is NOT optimized for multi-threaded access.<br>
 * This implementation is:
 * <ul>
 *     <li>Designed for single-threaded use only.</li>
 *     <li>Not thread-safe.</li>
 * </ul>
 * <p>
 * Due to the lack of thread-safety considerations, this implementation offers high performance in single-threaded environments.
 * </p>
 * <p>
 * Caution: Using this memory pool in a multithreaded environment will result in undefined behavior, including but not limited to data corruption and program crashes.
 * </p>
 */
public class SimpleMemoryPool implements IMemoryPool {

    public SimpleMemoryPool() {
    }

    @Override
    public long malloc(int size) {
        return 0;
    }

    @Override
    public void free(long address) {

    }

    @Override
    public long put(long pointer, byte[] data) {
        return 0;
    }

    @Override
    public long put(byte[] data) {
        return 0;
    }
}
