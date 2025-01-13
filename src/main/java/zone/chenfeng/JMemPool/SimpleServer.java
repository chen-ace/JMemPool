package zone.chenfeng.JMemPool;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

public class SimpleServer {

    public static void main(String[] args) throws Exception {
        SimpleServer simpleServer = new SimpleServer(9800,new StringMap());
        simpleServer.start();
    }

    int port;
    StringMap storage;
    public SimpleServer(int port, StringMap storage) {
        this.port = port;
        this.storage = storage;
    }

    public void start() throws Exception {
        // nio
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.socket().bind(new InetSocketAddress(this.port));
        serverChannel.configureBlocking(false);

        Selector selector = Selector.open();
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        ByteBuffer buffer = ByteBuffer.allocate(256);

        while (true) {
            selector.select();
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iter = selectedKeys.iterator();

            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                iter.remove();

                if (key.isAcceptable()) {
                    SocketChannel client = serverChannel.accept();
                    client.configureBlocking(false);
                    client.register(selector, SelectionKey.OP_READ);
                    System.out.println("Accepted new connection from " + client);
                }

                if (key.isReadable()) {
                    SocketChannel client = (SocketChannel) key.channel();
                    buffer.clear();
                    int r = client.read(buffer);

                    if (r <= 0) {
                        client.close();
                        continue;
                    }

                    buffer.flip();
                    String command = Charset.defaultCharset().decode(buffer).toString();
                    String response = processCommand(command.trim());

                    ByteBuffer writeBuffer = ByteBuffer.wrap(response.getBytes());
                    client.write(writeBuffer);
                }
            }
        }
    }

    private String processCommand(String command) {
        String[] parts = command.split("\\s+");
        if (parts.length == 0) return "Invalid command\n";

        switch (parts[0].toLowerCase()) {
            case "set":
                if (parts.length != 3) return "Invalid set command. Usage: set key value\n";
                storage.put(parts[1], parts[2]);
                return "OK\n";

            case "get":
                if (parts.length != 2) return "Invalid get command. Usage: get key\n";
                String value = storage.get(parts[1]);
                return value != null ? value + "\n" : "null\n";

            default:
                return "Unknown command\n";
        }
    }
}
