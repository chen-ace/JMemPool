package JMemPool;

public interface IMemoryPool extends AutoCloseable {

    long malloc(int size);
    void free(long pointer);
    long put(long pointer,byte[] data);
    long put(byte[] data);
    byte[] get(long pointer);
}
