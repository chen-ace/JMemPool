package JMemPool;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Scanner;

public class SimpleClient {

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            String command = scanner.nextLine();

            if (command.equalsIgnoreCase("quit")) {
                break;
            }

            try {
                String response = sendCommand(command);
                System.out.print(response);
            } catch (IOException e) {
                System.out.println("Error communicating with server: " + e.getMessage());
            }
        }

        scanner.close();
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
