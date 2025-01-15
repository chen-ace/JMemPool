# JMemPool

A high-performance off-heap memory pool for Java applications.

## Introduction

JMemPool is a Java library that provides a high-performance, off-heap memory pool implementation. It aims to minimize garbage collection (GC) overhead and improve memory management efficiency for applications that require frequent allocation and deallocation of memory, especially for large datasets or high-throughput scenarios. It's heavily inspired by memory management techniques used in high-performance systems like Redis and utilizes concepts similar to jemalloc.

## Why this project?

Traditional Java applications rely on the Java Virtual Machine (JVM) heap for memory allocation. While the JVM's garbage collector is effective in many cases, the inherent overhead of Java object representation can become a significant performance bottleneck, especially in scenarios with:

*   **Object Header Overhead:** Every Java object has an object header, which minimally contains:
    *   **Mark Word:** Stores information like hash code, GC generation age, lock status flags, thread ID, and timestamps.
    *   **Class Pointer:** Points to the object's class metadata.
    *   (For arrays) An array length field.

    This metadata consumes memory even for very small objects. In HotSpot VM, the object header typically occupies 12 bytes (without compressed pointers) or 8 bytes (with compressed pointers). This leads to considerable memory waste, especially when applications create numerous small objects. For example, an object storing only an `int` (4 bytes) requires at least 12 bytes, wasting 8 bytes (66% overhead).

*   **Reference Overhead:** Java uses references to access objects. Each reference itself consumes memory (typically 4 or 8 bytes).

*   **Frequent Object Creation/Destruction:** High rates of object allocation and deallocation not only increase GC pressure but also waste significant memory due to the per-object metadata overhead.

JMemPool addresses these challenges by providing an off-heap memory pool that bypasses the JVM's object model and directly manages raw memory. This offers several advantages:

*   **Significantly Improved Memory Efficiency:** Reduces the per-object overhead, enabling more efficient utilization of memory space.
*   **Reduced GC Pressure:** Decreases the number of objects requiring GC management, leading to lower GC frequency and pauses.
*   **Faster Memory Allocation and Access:** Direct memory manipulation is generally faster than accessing memory through Java object references.

## Typical Use Cases

JMemPool is suitable for a variety of applications, including:

*   **Caching:** Implementing high-performance caches that store large amounts of data in memory.
*   **Data processing:** Handling large datasets in memory for processing and analysis.
*   **Networking:** Buffering network data for high-throughput network applications.
*   **Game development:** Managing game state and assets in memory for real-time performance.
*   **Message queues/brokers:** Storing and managing messages in memory for high-throughput message processing.

## Key Features (Optional - Add as you implement)

*   **Chunk-based allocation:** Similar to jemalloc, memory is divided into chunks for efficient management.
*   **Thread-local caches:** Per-thread caches to minimize contention and improve concurrency.
*   **Memory alignment:** Ensures proper memory alignment for optimal CPU access.
*   **Memory fragmentation management:** Strategies to minimize memory fragmentation over time.
*   **API for allocating and freeing memory blocks of varying sizes.**
*   **Integration with ByteBuffer for easy data access.**

## Getting Started (Optional - Add as you implement)

(Provide instructions on how to include the library in a project, e.g., Maven/Gradle dependency, and basic usage examples.)

```xml
<dependency>
    <groupId>zone.chenfeng</groupId>
    <artifactId>jmem-pool</artifactId>
    <version>1.0.0</version>
</dependency>
```

```java
// Example usage (Illustrative)
IMemoryPool pool = new SimpleMemoryPool();
String s = "hello world";
byte[] sBytes = s.getBytes(StandardCharsets.UTF_8);
// malloc space
long pointer = pool.malloc(sBytes.length);
// put data to pool
pool.put(pointer, sBytes);
// update data in pool (maybe get a new pointer)
pointer = pool.put(pointer, "hello JMemPool".getBytes(StandardCharsets.UTF_8));
// get data from pool
byte[] data = pool.get(pointer);
String str = new String(data, StandardCharsets.UTF_8);
        System.out.println(str);
// free space
        pool.free(pointer);
// or put date directly
long pointer2 = pool.put("hello world".getBytes(StandardCharsets.UTF_8));
```
