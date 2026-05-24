package rf.ebanina.utils.io.cache.impl;

import rf.ebanina.utils.io.cache.IDataCache;
import rf.ebanina.utils.io.cache.policy.ICacheEvictionPolicy;

import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

public class FileDataCache
        implements IDataCache
{
    private final Path rootDir;
    private final ICacheEvictionPolicy evictionPolicy;

    public FileDataCache(String cacheDirectoryPath) {
        this(cacheDirectoryPath, null);
    }

    public FileDataCache(String cacheDirectoryPath, ICacheEvictionPolicy evictionPolicy) {
        this.rootDir = Paths.get(cacheDirectoryPath);
        this.evictionPolicy = evictionPolicy;

        try {
            Files.createDirectories(rootDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize cache directory: " + cacheDirectoryPath, e);
        }
    }

    @Override
    public void put(String key, byte[] data) {
        if (key == null || data == null)
            return;

        Path filePath = resolvePath(key);

        try {
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            if (evictionPolicy != null) {
                evictionPolicy.onPut(rootDir, filePath);
            }
        } catch (IOException e) {
            System.err.printf("utils.cache: Error writing key '%s' to disk. %s%n", key, e.getMessage());
        }
    }

    @Override
    public Optional<byte[]> get(String key) {
        if (key == null)
            return Optional.empty();

        Path filePath = resolvePath(key);
        if (!Files.exists(filePath)) {
            return Optional.empty();
        }

        if (evictionPolicy != null) {
            evictionPolicy.onGet(filePath);
        }

        try {
            return Optional.of(Files.readAllBytes(filePath));
        } catch (IOException e) {
            System.err.printf("utils.cache: Error reading key '%s' from disk. %s%n", key, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void remove(String key) {
        if (key == null)
            return;

        try {
            Files.deleteIfExists(resolvePath(key));
        } catch (IOException e) {
            System.err.printf("utils.cache: Error deleting key '%s'. %s%n", key, e.getMessage());
        }
    }

    @Override
    public void clear() {
        if (!Files.exists(rootDir))
            return;

        try (Stream<Path> walk = Files.walk(rootDir)) {
            walk.sorted(Comparator.reverseOrder())
                    .filter(path -> !path.equals(rootDir))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException ignored) {}
                    });
        } catch (IOException e) {
            System.err.println("utils.cache: Critical error during clear operational chain. " + e.getMessage());
        }
    }

    private Path resolvePath(String key) {
        String hex = hashMd5(key);

        return rootDir.resolve(hex.substring(0, 2))
                .resolve(hex.substring(2, 4))
                .resolve(hex);
    }

    private String hashMd5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder(32);

            for (byte b : messageDigest) {
                String h = Integer.toHexString(0xFF & b);
                if (h.length() == 1) hexString.append('0');
                hexString.append(h);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm is missing in this JVM environment", e);
        }
    }
}
