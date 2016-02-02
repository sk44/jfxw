package sk44.jfxw.model;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author sk
 */
class PathHistoriesCache {

    private static final float LOAD_FACTOR = 0.75f;
    private final LinkedHashMap<String, Path> cache;

    public PathHistoriesCache(int bufferSize) {
        this.cache = new LinkedHashMap<String, Path>(bufferSize, LOAD_FACTOR, false) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Path> eldest) {
                return size() > bufferSize;
            }
        };
    }

    public void put(Path path, Path focusedPath) {
        this.cache.put(keyOf(path), focusedPath);
    }

    public boolean contains(Path path) {
        return this.cache.containsKey(keyOf(path));
    }

    public Path lastFocusedIn(Path path) {
        return this.cache.get(keyOf(path));
    }

    private String keyOf(Path path) {
        return path.toString();
    }
}
