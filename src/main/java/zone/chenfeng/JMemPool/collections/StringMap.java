package zone.chenfeng.JMemPool.collections;

import java.util.HashMap;
import java.util.Map;

public class StringMap {
    Map<String, Long> map;
    StringPool pool;
    public StringMap() {
        map = new HashMap<>();
        pool = new StringPool();
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public boolean containsKey(String key) {
        return map.containsKey(key);
    }

    public String get(String key) {
        Long l = map.get(key);
        return pool.getString(l);
    }

    public Long put(String key, String value) {
        long pointer = pool.putString(value);
        return map.put(key, pointer);
    }

    public String remove(String key) {
        Long pointer = map.remove(key);
        return pool.free(pointer);
    }

}
