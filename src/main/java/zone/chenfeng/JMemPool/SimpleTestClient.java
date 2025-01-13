package zone.chenfeng.JMemPool;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

public class SimpleTestClient {

    public static void main(String[] args) throws Exception {
        int totalRequests = 1_0000;
        // 创建线程池
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(totalRequests);
        long l = System.nanoTime();
        // 并发发送请求
        for (int i = 0; i < totalRequests; i++) {
            executor.submit(() -> {
                try {
                    String key = randomString(5, 20);
                    String value = randomString(2, 60);
                    String r = sendCommand("set " + key + " " + value);
                    System.out.println(r + ": " + value);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        long l1 = System.nanoTime();
        long dt = l1 - l;
        System.out.println((double) dt/(1000*1000*1000));
    }

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

    private static String sendCommand(String command) throws IOException {
        // 每次执行命令建立新连接
        SocketChannel channel = SocketChannel.open();
        try {
            channel.connect(new InetSocketAddress("localhost", 9800));

            // 发送命令
            channel.write(ByteBuffer.wrap(command.getBytes()));

            // 读取响应
            ByteBuffer buffer = ByteBuffer.allocate(256);
            buffer.clear();
            channel.read(buffer);
            buffer.flip();

            return Charset.defaultCharset().decode(buffer).toString();
        } finally {
            channel.close(); // 确保连接关闭
        }
    }
}
