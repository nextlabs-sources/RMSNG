package com.nextlabs.rms.shared;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class CleanUpUtil {

    private static final Logger LOGGER = LogManager.getLogger(CleanUpUtil.class);

    private CleanUpUtil() {

    }

    public static void cleanUpFolder(File tempDir, final long inactiveDuration) {
        if (!tempDir.exists()) {
            LOGGER.warn("Cannot locate directory to clean up. Skipping this directory: " + tempDir.getAbsolutePath());
            return;
        }
        final Path start = Paths.get(tempDir.getAbsolutePath());
        try {
            Files.walkFileTree(start, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (dir.compareTo(start) != 0 && System.currentTimeMillis() - dir.toFile().lastModified() < inactiveDuration) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    // not deleting the files in root path, only directories (to avoid deleting ehcache files)
                    if (!start.equals(file.getParent()) && System.currentTimeMillis() - file.toFile().lastModified() > inactiveDuration) {
                        Files.delete(file);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException e) {
                    if (e == null) {
                        if (dir.compareTo(start) != 0) {
                            try {
                                Files.delete(dir);
                            } catch (IOException e1) {
                                LOGGER.error("Error occurred while deleting a directory : " + dir + e1.getMessage());
                                return FileVisitResult.CONTINUE;
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    } else {
                        // directory iteration failed
                        LOGGER.error("Directory Iteration Failed. Skipping this directory : " + dir);
                        return FileVisitResult.CONTINUE;
                    }
                }
            });
        } catch (IOException e) {
            LOGGER.error("Error occurred while deleting a directory in temp folder " + e.getMessage());
        }
    }
}
