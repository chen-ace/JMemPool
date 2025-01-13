package zone.chenfeng.JMemPool;


import zone.chenfeng.JMemPool.impl.SimpleMemoryPool;
import zone.chenfeng.JMemPool.utils.BenchmarkUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

public class Main {
    public static void main(String[] args) {
        System.out.println("基本测试开始\n");
        testBasic();
        System.out.println("\n等价测试开始\n");
        testEquals(2000,1,2048);
        System.out.println("\n性能测试开始...\n");
        testPerformance(10_0000,1,128);
        System.out.println("测试结束。");
    }

    public static void testBasic(){
        try(IMemoryPool pool = new SimpleMemoryPool()){
            String s = "hello world";
            byte[] sBytes = s.getBytes(StandardCharsets.UTF_8);
            // malloc space
            long pointer = pool.malloc(sBytes.length);
            // put data to pool
            pointer = pool.put(pointer, sBytes);
            // update data in pool (maybe get a new pointer)
            long pointer2 = pool.put(pointer, "hello JMemPool!More than 16 bytes, will get a new pointer".getBytes(StandardCharsets.UTF_8));
            assert pointer != pointer2;
            // get data from pool
            byte[] data = pool.get(pointer2);
            String str = new String(data, StandardCharsets.UTF_8);
            System.out.println(str);
            // free space
            pool.free(pointer2);
            // or put date directly
            long pointer3 = pool.put("hello world".getBytes(StandardCharsets.UTF_8));
            byte[] data3 = pool.get(pointer3);
            String str3 = new String(data3, StandardCharsets.UTF_8);
            System.out.println(str3);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 等价测试<br>
     * 测试字符串是否等价，即保存到内存池后再取出的的字符串和原始字符串是否相等
     * @param AMOUNT 测试数量
     * @param minSize 最小字符串长度 建议1
     * @param maxSize 最大字符串长度 建议2048
     */
    public static void testEquals(int AMOUNT,int minSize,int maxSize){
        try(IMemoryPool pool = new SimpleMemoryPool()){
            List<String> strs = new ArrayList<>();
            List<Long> collect = Stream.iterate(0, i -> i + 1).limit(AMOUNT).map(inx -> {
                // 随机字符串长度
                int size = ThreadLocalRandom.current().nextInt(minSize, maxSize);
                // 使用ThreadLocalRandom + ASCII字符
                StringBuilder sb = new StringBuilder(size);
                for(int i = 0; i < size; i++) {
                    // 生成可见ASCII字符 (33-126)
                    sb.append((char)(ThreadLocalRandom.current().nextInt(33, 127)));  // nextInt是前闭后开区间[33,127)
                }
                String str1 = sb.toString();
                strs.add(str1);
                return pool.put(str1.getBytes(StandardCharsets.UTF_8));
            }).toList();

            int errCount = 0;
            for (int i = 0; i < strs.size(); i++) {
                long p = collect.get(i);
                byte[] bytes = pool.get(p);
                String sfp = new String(bytes, StandardCharsets.UTF_8);
                String os = strs.get(i);
                if(!sfp.equals(os)){
                    errCount++;
                    pool.get(p);
                    System.out.printf("ERROR！ ORIGIN : %s but POOL : %s%n", os,sfp);
                }
            }
            assert errCount == 0;
            System.out.println("测试通过");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void testPerformance(int AMOUNT,int minSize,int maxSize){
        try(SimpleMemoryPool pool = new SimpleMemoryPool()){
            List<String> strs = new ArrayList<>();
            List<Long> collect = Stream.iterate(0, i -> i + 1).limit(AMOUNT).map(inx -> {
                // 随机字符串长度
                int size = ThreadLocalRandom.current().nextInt(minSize, maxSize);
                // 使用ThreadLocalRandom + ASCII字符
                StringBuilder sb = new StringBuilder(size);
                for(int i = 0; i < size; i++) {
                    // 生成可见ASCII字符 (33-126)
                    sb.append((char)(ThreadLocalRandom.current().nextInt(33, 127)));  // nextInt是前闭后开区间[33,127)
                }
                String str1 = sb.toString();
                strs.add(str1);
                return pool.put(str1.getBytes(StandardCharsets.UTF_8));
            }).toList();

            long usedBytes = pool.usedBytes();
            System.out.println("内存池占用的内存："+ usedBytes);
            System.out.println("内存池中的元素个数："+ pool.usedBlocks());
            long stringBytesCount = strs.parallelStream().map(s1 -> s1.getBytes(StandardCharsets.UTF_8).length).mapToLong(e -> e).sum();
            System.out.println("所有的String的byte占用的内存字节数："+ stringBytesCount);
            long memorySize = BenchmarkUtils.calculateListMemory(strs);
            System.out.println("Estimated JAVA memory size: " + memorySize + " bytes");

            System.out.println("内存池多占用了:"+ (usedBytes - stringBytesCount) + "字节");
            System.out.println("平均每个字符串多占用了:"+ (usedBytes - stringBytesCount)/AMOUNT + "字节");


            System.out.println("Java多占用了:"+ (memorySize - stringBytesCount) + "字节");
            System.out.println("平均每个字符串多占用了:"+ (memorySize - stringBytesCount)/AMOUNT + "字节");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}