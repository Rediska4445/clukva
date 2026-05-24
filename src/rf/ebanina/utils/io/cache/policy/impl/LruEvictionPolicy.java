package rf.ebanina.utils.io.cache.policy.impl;

import rf.ebanina.utils.io.cache.policy.ICacheEvictionPolicy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LruEvictionPolicy implements ICacheEvictionPolicy {
    private final long maxSizeBytes;
    private final double cleanupThreshold;

    private final Map<Path, Long> accessIndex = new ConcurrentHashMap<>();
    private final AtomicLong currentCacheSize = new AtomicLong(0);
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);

    private final AtomicBoolean isCleaningInProgress = new AtomicBoolean(false);

    private final ExecutorService backgroundCleaner = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "cache-lru-cleaner-daemon");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);

        return thread;
    });

    public LruEvictionPolicy(long maxSizeBytes, double cleanupThreshold) {
        this.maxSizeBytes = maxSizeBytes;
        this.cleanupThreshold = cleanupThreshold;
    }

    public LruEvictionPolicy(long maxSizeBytes) {
        this(maxSizeBytes, 0.8);
    }

    @Override
    public void onPut(Path rootDir, Path targetFile) {
        if (!Files.exists(rootDir))
            return;

        try {
            long currentSize;
            try (Stream<Path> stream = Files.walk(rootDir)) {
                currentSize = stream.filter(Files::isRegularFile)
                        .mapToLong(p -> p.toFile().length())
                        .sum();
            }

            if (currentSize <= maxSizeBytes) {
                return;
            }

            List<Path> filesSortedByLru;
            try (Stream<Path> stream = Files.walk(rootDir)) {
                filesSortedByLru = stream.filter(Files::isRegularFile)
                        .sorted(Comparator.comparingLong(p -> p.toFile().lastModified()))
                        .collect(Collectors.toList());
            }

            long targetSize = (long) (maxSizeBytes * cleanupThreshold);
            for (Path file : filesSortedByLru) {
                if (currentSize <= targetSize) {
                    break;
                }

                long fileSize = Files.size(file);
                if (Files.deleteIfExists(file)) {
                    currentSize -= fileSize;
                }
            }

        } catch (IOException e) {
            System.err.println("Cache Eviction: Error during LRU cleanup execution. " + e.getMessage());

            throw new RuntimeException(e);
        }
    }

    @Override
    public void onGet(Path targetFile) {
        if (Files.exists(targetFile)) {
            try {
                targetFile.toFile().setLastModified(System.currentTimeMillis());
            } catch (Exception ignored) {}
        }
    }
}
