package JMemPool;

public interface IMemoryPool {

    long malloc(int size);
    void free(long address);
    long put(long pointer,byte[] data);
    long put(byte[] data);
}
