package JMemPool;


import JMemPool.impl.SimpleMemoryPool;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    public static void main(String[] args) {
        IMemoryPool pool = new SimpleMemoryPool();
        String oldData = "hello world";
        byte[] newData = oldData.getBytes(StandardCharsets.UTF_8);
        long pointer = pool.malloc(newData.length);
        long pointer2 = pool.put(pointer, newData);
        long pointer3 = pool.put("pointer, newDatacxdsd".getBytes(StandardCharsets.UTF_8));
        byte[] getData = pool.get(pointer2);
        String strFromMP = new String(getData, StandardCharsets.UTF_8);
        System.out.println(strFromMP);

        List<String> strs = new ArrayList<>();
        List<Long> collect = Stream.iterate(0, i -> i + 1).limit(20000).map(inx -> {
            // 2~255随机数
            int size = ThreadLocalRandom.current().nextInt(1, 32);
            // 使用ThreadLocalRandom + ASCII字符
            StringBuilder sb = new StringBuilder(size);
            for(int i = 0; i < size; i++) {
                // 生成可见ASCII字符 (33-126)
                sb.append((char)(ThreadLocalRandom.current().nextInt(33, 127)));  // nextInt是前闭后开区间[33,127)
            }
            String str = sb.toString();
            strs.add(str);
            long p = pool.put(str.getBytes(StandardCharsets.UTF_8));
            return p;
        }).collect(Collectors.toList());

        int errCount = 0;
        for (int i = 0; i < strs.size(); i++) {
            Long p = collect.get(i);
            byte[] bytes = pool.get(p);
            String sfp = new String(bytes, StandardCharsets.UTF_8);
            String os = strs.get(i);
            if(!sfp.equals(os)){
                errCount++;
                System.out.println("ERROR！ ORIGIN : %s but POOL : %s".formatted(os,sfp));
            }
        }
        if(errCount > 0){
            System.out.println("测试不通过");
        }else{
            System.out.println("测试通过");
        }
        System.out.println("测试结束。");

        System.out.println(((SimpleMemoryPool) pool).usedBytes());
        System.out.println(((SimpleMemoryPool) pool).usedBlocks());
        System.out.println(strs.parallelStream().map(s -> s.getBytes(StandardCharsets.UTF_8).length).collect(Collectors.summingLong(e->e)));

    }
}