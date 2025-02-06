package zone.chenfeng.JMemPool.impl;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

public class OffHeapMemoryPool {

    private static final Unsafe UNSAFE;

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (Unsafe) field.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final long address;
    private final long size;

    public OffHeapMemoryPool(long size) {
        this.address = UNSAFE.allocateMemory(size);
        this.size = size;
    }

    public long getAddress() {
        return address;
    }

    public long getSize() {
        return size;
    }

    public void freeMemory() {
        UNSAFE.freeMemory(address);
    }

    public byte getByte(long offset) {
        return UNSAFE.getByte(address + offset);
    }

    public byte[] getBytes(long offset,int size) {
        byte[] bytes = new byte[size];
        for (int i = 0; i < size; i++) {
            byte aByte = UNSAFE.getByte(address + offset + i);
            bytes[i] = aByte;
        }
        return bytes;
    }


    public void putByte(long offset, byte value) {
        UNSAFE.putByte(address + offset, value);
    }

    public void putBytes(long offset, byte[] value) {
        for (byte b : value) {
            UNSAFE.putByte(address + offset++, b);
        }
    }


    // ... other get and put methods for different data types

    public static void main(String[] args) {
        OffHeapMemoryPool pool = new OffHeapMemoryPool(1024 * 1024); // Allocate 1MB

        // Use the pool...
        String str = "Hello, World!您好";
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);

        long offset = 0;
        int size = bytes.length;
        pool.putBytes(offset, bytes);
        byte[] readBytes = pool.getBytes(offset, size);
        String readStr = new String(readBytes, StandardCharsets.UTF_8);
        assert str.equals(readStr);
        System.out.println(readStr);

        String str2 = "这是使用UNSAFE实现的OffHeap内存池";
        byte[] bytes2 = str2.getBytes(StandardCharsets.UTF_8);
        offset = offset+size;
        size = bytes2.length;
        pool.putBytes(offset, bytes2);
        byte[] readBytes2 = pool.getBytes(offset, size);
        String readStr2 = new String(readBytes2, StandardCharsets.UTF_8);
        assert str2.equals(readStr2);
        assert !str.equals(readStr2);
        System.out.println(readStr2);

        pool.freeMemory(); // Release the memory when done
    }
}
