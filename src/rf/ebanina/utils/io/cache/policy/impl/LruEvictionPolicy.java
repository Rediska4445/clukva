package rf.ebanina.utils.io.cache.policy.impl;

import rf.ebanina.utils.io.cache.policy.ICacheEvictionPolicy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public class LruEvictionPolicy
        implements ICacheEvictionPolicy
{
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

    private void lazyInit(Path rootDir) {
        if (isInitialized.compareAndSet(false, true)) {
            if (!Files.exists(rootDir)) return;

            backgroundCleaner.submit(() -> {
                try (Stream<Path> stream = Files.walk(rootDir)) {
                    stream.filter(Files::isRegularFile).forEach(path -> {
                        long size = path.toFile().length();
                        long lastModified = path.toFile().lastModified();

                        accessIndex.put(path, lastModified);
                        currentCacheSize.addAndGet(size);
                    });
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Override
    public void onPut(Path rootDir, Path targetFile) {
        if (!Files.exists(rootDir))
            return;

        lazyInit(rootDir);

        try {
            long fileSize = Files.size(targetFile);

            Long oldTime = accessIndex.put(targetFile, System.currentTimeMillis());
            if (oldTime != null) {
                currentCacheSize.addAndGet(fileSize - targetFile.toFile().length());
            } else {
                currentCacheSize.addAndGet(fileSize);
            }

            if (currentCacheSize.get() > maxSizeBytes) {
                triggerBackgroundCleanup();
            }
        } catch (IOException e) {
            System.err.println("Cache Eviction: Error during LRU cleanup execution. " + e.getMessage());

            throw new RuntimeException(e);
        }
    }

    @Override
    public void onGet(Path targetFile) {
        accessIndex.computeIfPresent(targetFile, (path, oldTime) -> System.currentTimeMillis());
    }

    private void triggerBackgroundCleanup() {
        if (isCleaningInProgress.compareAndSet(false, true)) {
            backgroundCleaner.submit(() -> {
                try {
                    executeLruCleanup();
                } finally {
                    isCleaningInProgress.set(false);
                }
            });
        }
    }

    private void executeLruCleanup() {
        long currentSize = currentCacheSize.get();
        if (currentSize <= maxSizeBytes)
            return;

        long targetSize = (long) (maxSizeBytes * cleanupThreshold);

        java.util.List<Map.Entry<Path, Long>> sortedFiles = accessIndex.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .toList();

        for (Map.Entry<Path, Long> entry : sortedFiles) {
            if (currentSize <= targetSize) {
                break;
            }

            Path fileToDelete = entry.getKey();
            try {
                if (Files.exists(fileToDelete)) {
                    long size = Files.size(fileToDelete);

                    if (Files.deleteIfExists(fileToDelete)) {
                        currentSize -= size;
                        currentCacheSize.addAndGet(-size);
                        accessIndex.remove(fileToDelete);
                    }
                } else {
                    accessIndex.remove(fileToDelete);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
