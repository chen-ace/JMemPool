package zone.chenfeng.JMemPool.impl;

public interface Page {
    int freeSize();

    int malloc(int size);

    void free(int pageOffset);

    byte[] get(int pageOffset);

    int put(int pageOffset, byte[] data);

    int usedBytes();

    int usedBlock();

    int type();
    int size();
    int getGlobalPageNum();
    void setGlobalPageNum(int globalPageNum);

    /**
     * 获取该页支持存储的最小字节数
     * @return
     */
    int minLength();

    /**
     * 获取该页支持存储的最大字节数
     * @return
     */
    int maxLength();

    int LITTLE_PAGE_TYPE = 1;
    int HUGE_PAGE_TYPE = 2;
}
