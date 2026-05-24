package rf.ebanina.utils.io.cache.policy;

import java.nio.file.Path;

public interface ICacheEvictionPolicy {
    void onPut(Path rootDir, Path targetFile);

    void onGet(Path targetFile);
}
