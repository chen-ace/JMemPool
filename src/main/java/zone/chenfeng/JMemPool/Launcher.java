package zone.chenfeng.JMemPool;

import zone.chenfeng.JMemPool.benchmark.BenchmarkTool;
import zone.chenfeng.JMemPool.server.SimpleClient;
import zone.chenfeng.JMemPool.server.SimpleServer;


public class Launcher {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("请指定要运行的类:server、client、test、testClient");
            return;
        }

        switch (args[0].toLowerCase()) {
            case "test":
                Main.main(args);
                break;
            case "server":
                SimpleServer.main(args);
                break;
            case "client":
                SimpleClient.main(args);
                break;
            case "testclient":
                BenchmarkTool.main(args);
                break;
            default:
                System.out.println("未知的选项");
        }
    }
}

