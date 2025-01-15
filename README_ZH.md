# JMemPool

一个用于 Java 应用的高性能堆外内存池。

## 简介

JMemPool 是一个 Java 库，提供了一个高性能的、堆外内存池的实现。它的目标是最大限度地减少垃圾回收 (GC) 的开销，并提高需要频繁分配和释放内存的应用程序的内存管理效率，尤其适用于大型数据集或高吞吐量的场景。它深受 Redis 等高性能系统中使用的内存管理技术的启发，并采用了类似于 jemalloc 的概念。

## 为什么需要这个项目？
传统的 Java 应用程序依赖于 Java 虚拟机 (JVM) 堆进行内存分配。虽然 JVM 的垃圾回收器在许多情况下都很有效，但 Java 对象自身的实现方式导致了显著的元数据开销，这在某些场景下会成为性能瓶颈，尤其是在以下情况下：

*   **对象头开销：** 每个 Java 对象都有一个对象头（Object Header），至少包含以下信息：
    *   **Mark Word：** 存储对象的哈希码、GC 分代年龄、锁状态标志、线程 ID、时间戳等信息。
    *   **类型指针：** 指向对象的类元数据。
    *   如果对象是数组，还会有一个数组长度字段。

    这些信息即使对于非常小的对象也需要占用一定的内存空间，例如 HotSpot VM 中，对象头通常占用 12 字节（未启用压缩指针）或 8 字节（启用压缩指针）。这导致了显著的内存浪费，特别是当应用程序创建大量小对象时。例如，如果一个对象只需要存储一个 `int` 值（4 字节），那么加上对象头，至少需要 12 字节，浪费了 8 字节，内存利用率只有 33%。

*   **引用开销：** Java 中使用引用来操作对象。每个引用本身也需要占用一定的内存空间（通常是 4 字节或 8 字节）。

*   **频繁的对象创建和销毁：** 大量对象的创建和销毁不仅会导致 GC 压力增大，还会因为每个对象的元数据开销而浪费大量内存。

JMemPool 通过提供堆外内存池来解决这些挑战，它直接管理原始内存，避免了 Java 对象头的开销，从而：

*   **显著提高内存利用率：** 减少了每个对象所需的额外开销，可以更有效地利用内存空间。
*   **降低 GC 压力：** 减少了需要 GC 管理的对象数量，从而降低了 GC 频率和停顿时间。
*   **提高内存分配和访问速度：** 直接操作内存通常比通过 Java 对象引用进行访问更快。
## 典型使用场景

JMemPool 适用于各种应用程序，包括：

*   **缓存：** 实现将大量数据存储在内存中的高性能缓存。
*   **数据处理：** 在内存中处理大型数据集以进行处理和分析。
*   **网络：** 为高吞吐量网络应用程序缓冲网络数据。
*   **游戏开发：** 在内存中管理游戏状态和资源以实现实时性能。
*   **消息队列/代理：** 在内存中存储和管理消息以实现高吞吐量消息处理。

## 主要特性

*   **基于块的分配：** 类似于 jemalloc，内存被划分为块以进行有效管理。
*   **线程本地缓存：** 每个线程的本地缓存，以最大限度地减少争用并提高并发性。
*   **内存对齐：** 确保适当的内存对齐以实现最佳 CPU 访问。
*   **内存碎片管理：** 随着时间的推移最大限度地减少内存碎片的策略。
*   **用于分配和释放不同大小内存块的 API。**
*   **与 ByteBuffer 集成，方便数据访问。**

## 入门指南

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