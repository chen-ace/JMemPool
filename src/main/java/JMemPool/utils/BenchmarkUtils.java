package JMemPool.utils;

import java.util.List;

public class BenchmarkUtils {
    private static final int OBJECT_HEADER_SIZE = 12;  // 对象头大小(32位系统为8字节,64位系统为12字节)
    private static final int REFERENCE_SIZE = 4;       // 引用大小(32位系统为4字节,64位系统为8字节)
    private static final int ARRAY_HEADER_SIZE = 16;   // 数组头信息(32位系统为12字节,64位系统为16字节)

    public static long calculateListMemory(List<String> list) {
        if (list == null) return 0;

        // 1. ArrayList对象本身的大小
        long size = OBJECT_HEADER_SIZE;  // 对象头
        size += REFERENCE_SIZE;          // elementData数组的引用
        size += 3 * 4;                   // size, modCount, DEFAULT_CAPACITY 三个int字段

        // 2. 底层数组的大小
        int capacity = getArrayListCapacity(list);
        size += ARRAY_HEADER_SIZE;       // 数组头信息
        size += (long) capacity * REFERENCE_SIZE; // 数组中的所有引用

        // 3. 计算所有String对象的大小
        for (String str : list) {
            if (str != null) {
                size += calculateStringMemory(str);
            }
        }

        return size;
    }

    private static int getArrayListCapacity(List<?> list) {
        try {
            // 通过反射获取ArrayList的实际容量
            java.lang.reflect.Field field = list.getClass().getDeclaredField("elementData");
            field.setAccessible(true);
            Object[] elementData = (Object[]) field.get(list);
            return elementData.length;
        } catch (Exception e) {
            return list.size(); // 如果获取失败，返回size
        }
    }

    private static long calculateStringMemory(String str) {
        long size = OBJECT_HEADER_SIZE;   // String对象头
        size += REFERENCE_SIZE;           // value[] 引用
        size += 3 * 4;                    // hash(int), hash32(int), coder(byte) + 3 bytes padding

        // String的底层byte数组
        size += ARRAY_HEADER_SIZE;        // 数组头信息
        size += str.length();         // 每个char占2字节

        return size;
    }
}
