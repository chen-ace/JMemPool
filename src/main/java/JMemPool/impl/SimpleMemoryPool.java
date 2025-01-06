package JMemPool.impl;

import JMemPool.IMemoryPool;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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

    class Page {
        /**
         * 内存页
         */
        ByteBuffer buffer;
        /**
         * 页级别
         * 该页存储的最大内存块大小
         */
        short level;
        /**
         * 该页剩余空间
         */
        short free;
        /**
         * 该页最后分配的最大的内存块索引
         */
        short maxAllocIndex;
        /**
         * 业内的内存块是否被占用
         * 0表示空闲，1表示占用，位图
         */
        long[] freeArray;

        private static final int ARRAY_SIZE = 2048;
        private static final int BITS_PER_LONG = 64;
        private static final int BITMAP_SIZE = ARRAY_SIZE / BITS_PER_LONG;// 向上取整


        private Page(short level) {
            buffer = ByteBuffer.allocateDirect(level*ARRAY_SIZE);
            level = level;
            free = ARRAY_SIZE;
            freeArray = new long[BITMAP_SIZE];
            maxAllocIndex = 0;
        }

        protected Page new4BytePage() {
            return new Page((short) 4);
        }

        protected Page new8BytePage() {
            return new Page((short) 8);
        }

        protected Page new16BytePage() {
            return new Page((short) 16);
        }
        protected Page new32BytePage() {
            return new Page((short) 32);
        }
        protected Page new64BytePage() {
            return new Page((short) 64);
        }
        protected Page new128BytePage() {
            return new Page((short) 128);
        }
        protected Page new256BytePage() {
            return new Page((short) 256);
        }

        protected int malloc(int size) {
            if(free == 0) {
                // 该页已经没有空间了
                return -1;
            }
            short allocIndex = 0;
            if(maxAllocIndex < 2047) {
                // 该页还有空间，且不需要找碎片
                maxAllocIndex++;
                allocIndex = maxAllocIndex;
            }else {
                // 该页虽然还有空间，但空闲空间是碎片，需要从freeArray中找碎片
                allocIndex = (short) findFirstFree();
            }
            free--;
            setBit(allocIndex);
            return pack(allocIndex, size);
        }

        protected void free(int index) {
            clearBit(index);
            free ++;
        }

        // 设置某个位置为已占用
        public void setBit(int position) {
            if (position < 0 || position >= ARRAY_SIZE) {
                throw new IllegalArgumentException("Position out of range");
            }
            int index = position / BITS_PER_LONG;
            int bitPosition = position % BITS_PER_LONG;
            freeArray[index] |= (1L << bitPosition);
        }

        // 设置某个位置为空闲
        public void clearBit(int position) {
            if (position < 0 || position >= ARRAY_SIZE) {
                throw new IllegalArgumentException("Position out of range");
            }
            int index = position / BITS_PER_LONG;
            int bitPosition = position % BITS_PER_LONG;
            freeArray[index] &= ~(1L << bitPosition);
        }

        // 检查某个位置是否被占用
        public boolean isBitSet(int position) {
            if (position < 0 || position >= ARRAY_SIZE) {
                throw new IllegalArgumentException("Position out of range");
            }
            int index = position / BITS_PER_LONG;
            int bitPosition = position % BITS_PER_LONG;
            return (freeArray[index] & (1L << bitPosition)) != 0;
        }

        // 查找第一个空闲位置
        public int findFirstFree() {
            for (int i = 0; i < BITMAP_SIZE; i++) {
                if (freeArray[i] != -1L) { // -1L 表示所有位都是1
                    // 找到第一个0位
                    int bitPosition = Long.numberOfTrailingZeros(~freeArray[i]);
                    int result = i * BITS_PER_LONG + bitPosition;
                    return result < ARRAY_SIZE ? result : -1;
                }
            }
            return -1;
        }

        // 掩码常量
        private static final int SIZE_MASK = 0xFF;           // 8位全1
        private static final int INDEX_MASK = 0x7FF;         // 11位全1 (0111 1111 1111)
        private static final int INDEX_SHIFT = 8;            // index在第8位开始

        // 打包数据
        public static int pack(int index, int size) {
            if (size < 0 || size > 255) {
                throw new IllegalArgumentException("Size must be between 0 and 255");
            }
            if (index < 0 || index > 2048) {
                throw new IllegalArgumentException("Index must be between 0 and 2048");
            }

            return (index & INDEX_MASK) << INDEX_SHIFT | (size & SIZE_MASK);
        }

        // 解出index
        public static int getIndex(int packed) {
            return (packed >> INDEX_SHIFT) & INDEX_MASK;
        }

        // 解出size
        public static int getSize(int packed) {
            return packed & SIZE_MASK;
        }
    }
    List<Page> pages;
    List<Page> level2Pages8k;
    List<Page> level3Pages16k;
    List<Page> level4Pages32k;
    List<Page> level5Pages64k;
    List<Page> level6Pages128k;
    List<Page> level7Pages256k;
    List<Page> level8Pages512k;

    public SimpleMemoryPool() {
        pages = new ArrayList<>();
        level2Pages8k = new LinkedList<>();
        level3Pages16k = new LinkedList<>();
        level4Pages32k = new LinkedList<>();
        level5Pages64k = new LinkedList<>();
        level6Pages128k = new LinkedList<>();
        level7Pages256k = new LinkedList<>();
        level8Pages512k = new LinkedList<>();
    }

    // 向上取整到2的幂次方的指数
    public static int ceilToPowerOf2Exponent(int size) {
        if (size <= 0 || size > 255) {
            throw new IllegalArgumentException("Size must be between 1 and 255");
        }
        // size <= 4 时返回2
        if (size <= 4) return 2;

        // 计算大于等于size的最小2的幂的指数
        return 8 - Integer.numberOfLeadingZeros(size - 1) & 0xFF;
    }

    @Override
    public long malloc(int size) {
        int level = ceilToPowerOf2Exponent(size);

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
