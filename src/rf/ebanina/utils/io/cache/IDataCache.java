package rf.ebanina.utils.io.cache;

import java.util.Optional;

public interface IDataCache {

    void put(String key, byte[] data);

    Optional<byte[]> get(String key);

    void remove(String key);

    void clear();
}