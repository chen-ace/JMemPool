package zone.chenfeng.JMemPool.impl;

import zone.chenfeng.JMemPool.IMemoryPool;

import java.nio.ByteBuffer;
import java.util.*;
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
public class SimpleMemoryPool implements IMemoryPool{

    @Override
    public void close() throws Exception {
        pages.clear();
        levelPages.forEach(List::clear);
        hugeDataPages.forEach(e -> e.dataList.clear());
        levelPages=null;
        hugeDataPages.clear();
    }

    static class LittlePage implements Page {
        /**
         * 内存页
         */
        private ByteBuffer buffer;
        /**
         * 页级别
         * 该页存储的最大内存块大小,即2^level
         */
        private short level;
        /**
         * 该页剩余空间
         */
        private short free;
        /**
         * 该页最后分配的最大的内存块索引
         */
        private short maxAllocIndex;
        /**
         * 业内的内存块是否被占用
         * 0表示空闲，1表示占用，位图
         */
        private long[] freeArray;
        private int globalPageNum;

        private static final int ARRAY_SIZE = 2048;
        private static final int BITS_PER_LONG = 64;
        private static final int BITMAP_SIZE = ARRAY_SIZE / BITS_PER_LONG;// 向上取整


        @Override
        public int freeSize(){
            return free;
        }

        private LittlePage(short level) {
            buffer = ByteBuffer.allocateDirect((1<<level)*ARRAY_SIZE);
            this.level = level;
            free = ARRAY_SIZE;
            freeArray = new long[BITMAP_SIZE];
            maxAllocIndex = 0;
        }

        @Override
        public int malloc(int size) {
            if(free == 0) {
                // 该页已经没有空间了
                return -1;
            }
            short allocIndex;
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
            // 将对应位置的内存块置为0，避免malloc后未put就free
            int start = allocIndex << level;
            buffer.position(start);
            buffer.put(new byte[size]);

            return pack(allocIndex, size);
        }

        @Override
        public void free(int pageOffset) {
            int index = getIndex(pageOffset);
            clearBit(index);
            free ++;
        }

        @Override
        public byte[] get(int pageOffset) {
            int index = getIndex(pageOffset);
            int size = getSize(pageOffset);
            int start = index << level;
            byte[] result = new byte[size];
            buffer.position(start);
            buffer.get(result,0,size);
            return result;
        }

        @Override
        public int put(int pageOffset, byte[] data) {
            int index = getIndex(pageOffset);
            int size = data.length;
            int start = index << level;
            buffer.position(start);
            buffer.put(data);

            return pack(index,size);
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
            if (size < 0 || size > 256) {
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
            // 如果size为0，表示256 size不可能为0，且真的能存储256字节。所以这里返回256
            return (packed & SIZE_MASK) ==0? 256 : (packed & SIZE_MASK);
        }

        @Override
        public int usedBytes(){
            return (ARRAY_SIZE-free)*(1<<level);
        }

        @Override
        public int usedBlock(){
            return size();
        }

        @Override
        public int type() {
            return LITTLE_PAGE_TYPE;
        }

        @Override
        public int size() {
            return (ARRAY_SIZE-free);
        }

        @Override
        public int getGlobalPageNum() {
            return globalPageNum;
        }

        @Override
        public void setGlobalPageNum(int globalPageNum) {
            this.globalPageNum = globalPageNum;
        }

        @Override
        public int minLength() {
            if(level <= 2)
                return 1;
            return (1<<(level-1))+1;
        }

        @Override
        public int maxLength() {
            return 1<<level;
        }
    }

    static class HugePage implements Page{
        private List<byte[]> dataList;
        static int MAX_SIZE = 1<<20;
        private int globalPageNum;

        @Override
        public int freeSize(){
            return MAX_SIZE - dataList.size();
        }

        public HugePage(){
            dataList = new ArrayList<>();
        }

        @Override
        public int malloc(int size){
            dataList.add(EMPTY_DATA);
            return dataList.size()-1;
        }

        @Override
        public int put(int pageOffset, byte[] data){
            dataList.set(pageOffset,data);
            return pageOffset;
        }

        @Override
        public byte[] get(int pageOffset){
            return dataList.get(pageOffset);
        }

        @Override
        public void free(int pageOffset){
            // 设置为null后，GC会自动回收byte[]，不需要手动释放
            // 但是会浪费数组的这个index，如果需要优化，可以考虑使用一个标记数组，标记哪些index是空闲的
            // 此处不做优化，因为做这个优化的价值不大，而且会增加复杂度
            // 浪费一个index最多只会占用一个指针的内存，后续可以看实际使用情况，如果有必要再优化
            dataList.set(pageOffset,null);
        }

        public int put(byte[] data){
            dataList.add(data);
            return dataList.size()-1;
        }

        @Override
        public int usedBytes(){
            return dataList.parallelStream().map(e -> e.length).collect(Collectors.summingInt(e -> e));
        }

        @Override
        public int usedBlock(){
            return size();
        }

        @Override
        public int type() {
            return HUGE_PAGE_TYPE;
        }

        @Override
        public int size() {
            return (int)dataList.parallelStream().filter(Objects::nonNull).count();
        }

        @Override
        public int getGlobalPageNum() {
            return globalPageNum;
        }

        @Override
        public void setGlobalPageNum(int globalPageNum) {
            this.globalPageNum = globalPageNum;
        }

        @Override
        public int minLength() {
            return 257;
        }

        @Override
        public int maxLength() {
            return Integer.MAX_VALUE;
        }

    }
    List<Page> pages;
    List<List<Page>> levelPages;
    List<HugePage> hugeDataPages;

    public static byte[] EMPTY_DATA = new byte[0];

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
        hugeDataPages = new ArrayList<>();
    }

    /**
     * 向上取整到2的幂次方的指数
     *
     * @param size 要计算指数的整数
     * @return 大于或等于 size 的最小2的幂次方的指数
     * @throws IllegalArgumentException 如果 size 小于等于0或大于256
     */
    public static int ceilToPowerOf2Exponent(int size) {
        // 检查 size 是否在合法的范围内，即大于0且小于等于256
        if (size <= 0 || size > 256) {
            // 如果 size 不在合法范围内，抛出 IllegalArgumentException 异常
            throw new IllegalArgumentException("Size must be between 1 and 256");
        }
        // 如果 size 小于等于4，直接返回2，因为2的2次方（即4）是大于等于1且小于等于4的最小2的幂次方
        if (size <= 4) return 2;

        // 使用位操作计算2的幂次方的指数
        return 32 - Integer.numberOfLeadingZeros(size - 1);
    }


    @Override
    public long malloc(int size) {
        if(size <= 256) {
            int levelIndex = ceilToPowerOf2Exponent(size);
            List<Page> level = levelPages.get(levelIndex);
            Page freeLittlePage = level.parallelStream().filter(e -> e.freeSize() > 0).findFirst().orElseGet(() -> {
                LittlePage page = new LittlePage((short) levelIndex);
                level.add(page);
                pages.addLast(page);
                page.setGlobalPageNum(pages.size() - 1);
                return page;
            });
            int pageOffset = freeLittlePage.malloc(size);
            return packData(freeLittlePage.type(), freeLittlePage.getGlobalPageNum(), pageOffset);
        }else{
            // 大于256的数据，直接放入hugeData
            HugePage freeHugePage = hugeDataPages.parallelStream().filter(e -> e.freeSize() > 0).findFirst().orElseGet(() -> {
                HugePage page = new HugePage();
                hugeDataPages.add(page);
                pages.addLast(page);
                page.setGlobalPageNum(pages.size()-1);
                return page;
            });
            int pageOffset = freeHugePage.malloc(size);
            return packData(freeHugePage.type(), freeHugePage.getGlobalPageNum(), pageOffset);
        }
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
//        int type = getType(pointer);
        int pageNum = getPageNum(pointer);
        int offset = getOffset(pointer);
        Page page = pages.get(pageNum);
        if(data.length > page.maxLength()) {
            // 新数据长度大于原数据长度,且>原始页面的最大数据长度，需要重新在其他页面分配
            free(pointer);
            long new_p = malloc(data.length);
            int newPageNum = getPageNum(new_p);
            int newOffset = getOffset(new_p);
            Page newPage = pages.get(newPageNum);
            newPage.put(newOffset, data);
            return packData(newPage.type(), newPageNum, newOffset);
        }
        page.put(offset,data);
        return packData(page.type(),pageNum,offset);
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
