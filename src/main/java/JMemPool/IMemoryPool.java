package JMemPool;

public interface IMemoryPool {

    long malloc(int size);
    void free(long pointer);
    long put(long pointer,byte[] data);
    long put(byte[] data);
    byte[] get(long pointer);
}
