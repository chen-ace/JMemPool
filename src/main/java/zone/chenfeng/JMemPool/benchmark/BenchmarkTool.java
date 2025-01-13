package zone.chenfeng.JMemPool.benchmark;

import zone.chenfeng.JMemPool.server.IClient;
import zone.chenfeng.JMemPool.server.SimpleClient;
import zone.chenfeng.JMemPool.utils.StringUtils;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class BenchmarkTool {

    public static void main(String[] args) throws Exception {
        String host ="127.0.0.1";
        int port = 9800;

        if(args.length == 3){
            // args[0]为client
            host = args[1];
            port = Integer.parseInt(args[2]);
        }

        Scanner scanner = new Scanner(System.in);
        BenchmarkTool benchmarkTool = new BenchmarkTool(10,10000,2,32,"127.0.0.1",9800);
        benchmarkTool.runTest();
    }

    private final int threadCount;
    private final int requestPerThread;
    private int valueMinSize = 2;
    private int valueMaxSize = 32;
    private final AtomicLong totalTime = new AtomicLong(0);
    private final AtomicLong successCount = new AtomicLong(0);
    private final List<Long> latencies = new CopyOnWriteArrayList<>();
    String host;
    int port;

    public BenchmarkTool(int threadCount, int requestPerThread, int valueMinSize,int valueMaxSize,String host, int port) {
        this.threadCount = threadCount;
        this.requestPerThread = requestPerThread;
        this.valueMinSize = valueMinSize;
        this.valueMaxSize = valueMaxSize;
        this.host = host;
        this.port = port;
    }

    public void runTest() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        long startTime = System.currentTimeMillis();

        // 创建测试任务
        for (int i = 0; i < threadCount; i++) {
            IClient client = new SimpleClient(host,port);
            executor.submit(() -> {
                try {
                    runSingleThread(client);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long endTime = System.currentTimeMillis();
        executor.shutdown();

        // 打印结果
        printResults(endTime - startTime);
    }

    private void runSingleThread(IClient client) {
        String value = StringUtils.randomString(valueMinSize,valueMaxSize);

        for (int i = 0; i < requestPerThread; i++) {
            String key = "key-" + Thread.currentThread().threadId() + "-" + i;

            long start = System.nanoTime();
            try {
                // 执行SET操作
                client.set(key, value);
                // 执行GET操作
                client.get(key);

                successCount.incrementAndGet();
            } catch (Exception e) {
                e.printStackTrace();
            }
            long end = System.nanoTime();

            latencies.add((end - start) / 1000); // 转换为微秒
            totalTime.addAndGet(end - start);
        }
    }

    private void printResults(long totalTimeMillis) {
        long totalRequests = (long) threadCount * requestPerThread;
        double qps = (double) successCount.get() / (totalTimeMillis / 1000.0);

        // 计算延迟统计
        List<Long> sortedLatencies = new ArrayList<>(latencies);
        sortedLatencies.sort(Long::compareTo);

        long p95 = sortedLatencies.get((int) (sortedLatencies.size() * 0.95));
        long p99 = sortedLatencies.get((int) (sortedLatencies.size() * 0.99));

        System.out.println("=== Benchmark Results ===");
        System.out.println("Total Requests: " + totalRequests);
        System.out.println("Successful Requests: " + successCount.get());
        System.out.println("QPS: " + String.format("%.2f", qps));
        System.out.println("Average Latency: " +
                String.format("%.2f", totalTime.get() / (double) successCount.get() / 1000000) + "ms");
        System.out.println("P95 Latency: " + p95 + "μs");
        System.out.println("P99 Latency: " + p99 + "μs");
    }
}
