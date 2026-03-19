
package utilities;

import java.io.File;

public class AssetPathResolver {

    private static String cachedAssetsPath = null;
    private static String cachedDataFilePath = null;

    /**
     * Returns the absolute path to the assets directory.
     */
    public static String getAssetsPath() {
        if (cachedAssetsPath != null) {
            return cachedAssetsPath;
        }

        String appRoot = getApplicationRoot();
        File assetsDir = new File(appRoot, "assets");

        if (!assetsDir.exists() || !assetsDir.isDirectory()) {
            throw new RuntimeException(
                    "Assets directory not found at: " + assetsDir.getAbsolutePath()
            );
        }

        cachedAssetsPath = assetsDir.getAbsolutePath();
        return cachedAssetsPath;
    }

    /**
     * Returns the absolute path to assets/data.txt
     */
    public static String getDataFilePath() {
        if (cachedDataFilePath != null) {
            return cachedDataFilePath;
        }

        File dataFile = new File(getAssetsPath(), "data.txt");

        if (!dataFile.exists() || !dataFile.isFile()) {
            throw new RuntimeException(
                    "data.txt not found at: " + dataFile.getAbsolutePath()
            );
        }

        cachedDataFilePath = dataFile.getAbsolutePath();
        return cachedDataFilePath;
    }

    /**
     * Finds the application root directory by backtracking.
     */
    private static String getApplicationRoot() {
        try {
            String path = AssetPathResolver.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                    .getPath();

            File location = new File(path);

            // Running from JAR
            if (location.isFile()) {
                return location.getParent();
            }

            // Running from IDE: backtrack until assets folder is found
            File current = location;

            while (current != null) {
                File assetsDir = new File(current, "assets");
                if (assetsDir.exists() && assetsDir.isDirectory()) {
                    return current.getAbsolutePath();
                }
                current = current.getParentFile();
            }

            // Fallback
            return System.getProperty("user.dir");

        } catch (Exception e) {
            e.printStackTrace();
            return System.getProperty("user.dir");
        }
    }

    /**
     * Optional sanity check
     */
    public static void validateAssets() {
        try {
            System.out.println("Validating assets...");
            System.out.println("Assets path: " + getAssetsPath());
            System.out.println("✓ data.txt found at: " + getDataFilePath());
        } catch (RuntimeException e) {
            System.err.println("✗ Asset validation failed: " + e.getMessage());
            throw e;
        }
    }
}
