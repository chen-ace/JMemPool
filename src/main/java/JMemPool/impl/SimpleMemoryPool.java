package JMemPool.impl;

import JMemPool.IMemoryPool;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

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
         * 该页存储的最大内存块大小,即2^level
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
        int globalPageNum;

        private static final int ARRAY_SIZE = 2048;
        private static final int BITS_PER_LONG = 64;
        private static final int BITMAP_SIZE = ARRAY_SIZE / BITS_PER_LONG;// 向上取整


        private Page(short level) {
            buffer = ByteBuffer.allocateDirect((1<<level)*ARRAY_SIZE);
            this.level = level;
            free = ARRAY_SIZE;
            freeArray = new long[BITMAP_SIZE];
            maxAllocIndex = 0;
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

        protected void free(int pageOffset) {
            int index = getIndex(pageOffset);
            clearBit(index);
            free ++;
        }

        protected byte[] get(int pageOffset) {
            int index = getIndex(pageOffset);
            int size = getSize(pageOffset);
            int start = index << level;
            byte[] result = new byte[size];
            buffer.position(start);
            buffer.get(result,0,size);
            return result;
        }

        protected int put(int pageOffset, byte[] data) {
            int index = getIndex(pageOffset);
            int size = data.length;
            int start = index << level;
            buffer.position(start);
            buffer.put(data);

            int newPageOffset = pack(index,size);
            return newPageOffset;
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

        protected int usedBytes(){
            return (ARRAY_SIZE-free)*(1<<level);
        }

        protected int usedBlock(){
            return (ARRAY_SIZE-free);
        }
    }
    List<Page> pages;
    List<List<Page>> levelPages;

    public SimpleMemoryPool() {
        pages = new ArrayList<>();
        List<Page> level2Pages8k = new LinkedList<>();
        List<Page> level3Pages16k = new LinkedList<>();
        List<Page> level4Pages32k = new LinkedList<>();
        List<Page> level5Pages64k = new LinkedList<>();
        List<Page> level6Pages128k = new LinkedList<>();
        List<Page> level7Pages256k = new LinkedList<>();
        List<Page> level8Pages512k = new LinkedList<>();
        // level<2时，也放入level2Pages8k
        levelPages= Arrays.asList(level2Pages8k,level2Pages8k,level2Pages8k,level3Pages16k,level4Pages32k,level5Pages64k,level6Pages128k,level7Pages256k,level8Pages512k);
    }

    // 向上取整到2的幂次方的指数
    public static int ceilToPowerOf2Exponent(int size) {
        if (size <= 0 || size > 255) {
            throw new IllegalArgumentException("Size must be between 1 and 255");
        }
        if (size <= 4) return 2;

        int exp = 0;
        int value = 1;
        while (value < size) {
            value <<= 1;
            exp++;
        }
        return exp;
    }

    @Override
    public long malloc(int size) {
        int levelIndex = ceilToPowerOf2Exponent(size);
        List<Page> level = levelPages.get(levelIndex);
        Page freePage = level.parallelStream().filter(e->e.free>0).findFirst().orElseGet(()->{
            Page page = new Page((short) levelIndex);
            level.add(page);
            pages.addLast(page);
            page.globalPageNum = pages.size()-1;
            return page;
        });
        int pageOffset = freePage.malloc(size);
        return packData(1, freePage.globalPageNum,pageOffset);
    }

    // 打包函数
    public static long packData(int type, int pageNum, int offset) {
        // 确保输入在合法范围内
        type &= 0xF;  // 4位
        pageNum &= 0xFFFFFF; // 24位
        offset &= 0xFFFFF; // 20位

        // 左移并组合
        return ((long)type << 44) | ((long)pageNum << 20) | offset;
    }

    // 解包函数 - 获取类型
    public static int getType(long packed) {
        return (int)((packed >> 44) & 0xF);
    }

    // 解包函数 - 获取页号
    public static int getPageNum(long packed) {
        return (int)((packed >> 20) & 0xFFFFFF);
    }

    // 解包函数 - 获取偏移量
    public static int getOffset(long packed) {
        return (int)(packed & 0xFFFFF);
    }

    @Override
    public void free(long pointer) {
        int pageNum = getPageNum(pointer);
        int offset = getOffset(pointer);
        Page page = pages.get(pageNum);
        page.free(offset);
    }

    @Override
    public long put(long pointer, byte[] data) {
        if(data.length > 255) {
            throw new IllegalArgumentException("Data length must be between 0 and 255");
        }
        int pageNum = getPageNum(pointer);
        int offset = getOffset(pointer);
        Page page = pages.get(pageNum);
        if(data.length > (1<<page.level)) {
            // 新数据长度大于原数据长度,且>原始页面的最大数据长度，需要重新在其他页面分配
            free(pointer);
            long new_p = malloc(data.length);
            int newPageNum = getPageNum(new_p);
            int newOffset = getOffset(new_p);
            Page newPage = pages.get(newPageNum);
            newPage.put(newOffset,data);
            return packData(1,newPageNum,newOffset);
        }
        page.put(offset,data);
        return packData(1,pageNum,offset);
    }

    @Override
    public long put(byte[] data) {
        long pointer = malloc(data.length);
        return put(pointer,data);
    }

    @Override
    public byte[] get(long pointer) {
        int pageNum = getPageNum(pointer);
        int offset = getOffset(pointer);
        Page page = pages.get(pageNum);
        return page.get(offset);
    }

    public long usedBytes() {
        return pages.parallelStream().map(e -> (long) e.usedBytes()).collect(Collectors.summingLong(e -> e));
    }

    public long usedBlocks(){
        return pages.parallelStream().map(e -> (long) e.usedBlock()).collect(Collectors.summingLong(e -> e));
    }

    public List<Long> levelUsedBytes() {
         return levelPages.stream()
                 .map(t -> t.parallelStream().map(e -> (long) e.usedBytes()).collect(Collectors.summingLong(e -> e)))
                 .collect(Collectors.toList());

    }

    public List<Long> levelUsedBlocks(){
        return levelPages.stream()
                .map(t -> t.parallelStream().map(e -> (long) e.usedBlock()).collect(Collectors.summingLong(e -> e)))
                .collect(Collectors.toList());
    }
}
