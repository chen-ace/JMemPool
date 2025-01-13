package zone.chenfeng.JMemPool.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class SimpleClient implements IClient {

    public static void main(String[] args) throws Exception {

        String host ="127.0.0.1";
        int port = 9800;

        if(args.length == 3){
            // args[0]为client
            host = args[1];
            port = Integer.parseInt(args[2]);
        }

        Scanner scanner = new Scanner(System.in);
        SimpleClient client = new SimpleClient(host, port);
        while (true) {
            System.out.print("> ");
            String command = scanner.nextLine();

            if (command.equalsIgnoreCase("quit")) {
                client.close();
                break;
            }

            try {
                String response = client.send(command);
                System.out.print(response);
            } catch (IOException e) {
                System.out.println("Error communicating with server: " + e.getMessage());
            }
        }

        scanner.close();
    }

    public String send(String command) throws Exception {
        // 发送命令
        channel.write(ByteBuffer.wrap(command.getBytes()));

        // 读取响应
        ByteBuffer buffer = ByteBuffer.allocate(256);
        buffer.clear();
        channel.read(buffer);
        buffer.flip();

        return Charset.defaultCharset().decode(buffer).toString();
    }

    SocketChannel channel;
    public SimpleClient(String host, int port) throws Exception {
        channel = SocketChannel.open();
        channel.connect(new InetSocketAddress(host, port));
    }

    // 使用Jedis或Lettuce实现
    @Override
    public String set(String key, String value) {
        try {// 实现Redis SET操作
            String command = "set " + key + " " + value;
            // 发送命令
            channel.write(ByteBuffer.wrap(command.getBytes(StandardCharsets.UTF_8)));

            // 读取响应
            ByteBuffer buffer = ByteBuffer.allocate(256);
            buffer.clear();
            channel.read(buffer);
            buffer.flip();

            return Charset.defaultCharset().decode(buffer).toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String get(String key) {
        try {// 实现Redis SET操作
            String command = "get " + key;
            // 发送命令
            channel.write(ByteBuffer.wrap(command.getBytes(StandardCharsets.UTF_8)));

            // 读取响应
            ByteBuffer buffer = ByteBuffer.allocate(256);
            buffer.clear();
            channel.read(buffer);
            buffer.flip();

            return Charset.defaultCharset().decode(buffer).toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        try {
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
