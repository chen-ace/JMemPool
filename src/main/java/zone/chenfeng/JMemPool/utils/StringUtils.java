package zone.chenfeng.JMemPool.utils;

import java.util.concurrent.ThreadLocalRandom;

public class StringUtils {
    public static String randomString(int minSize, int maxSize) {
        // 随机字符串长度
        int size = ThreadLocalRandom.current().nextInt(minSize, maxSize);
        // 使用ThreadLocalRandom + ASCII字符
        StringBuilder sb = new StringBuilder(size);
        for(int i = 0; i < size; i++) {
            // 生成可见ASCII字符 (33-126)
            sb.append((char)(ThreadLocalRandom.current().nextInt(33, 127)));  // nextInt是前闭后开区间[33,127)
        }
        return sb.toString();
    }
}
