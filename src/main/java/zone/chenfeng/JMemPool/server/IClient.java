package zone.chenfeng.JMemPool.server;

public interface IClient extends AutoCloseable{
    String set(String key, String value);
    String get(String key);
    String send(String cmd) throws Exception;
}
