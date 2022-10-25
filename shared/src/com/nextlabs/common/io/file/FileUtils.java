package com.nextlabs.common.io.file;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntry.Builder;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class FileUtils {

    /**
     * Cleans a directory without deleting it.
     *
     * @param directory
     * @throws IOException
     */
    public static void cleanDirectory(final File directory) throws IOException {
        File[] files = listFiles(directory);
        if (files != null) {
            for (File file : files) {
                forceDelete(file);
            }
        }
    }

    /**
     * Deletes a directory recursively.
     *
     * @param directory
     * @throws IOException
     */
    public static void deleteDirectory(final File directory) throws IOException {
        if (!directory.exists()) {
            return;
        }
        cleanDirectory(directory);
        if (!directory.delete()) {
            final String message = "Unable to delete directory " + directory + ".";
            throw new IOException(message);
        }
    }

    public static boolean deleteQuietly(final File file) {
        if (file == null) {
            return false;
        }
        try {
            if (file.isDirectory()) {
                cleanDirectory(file);
            }
        } catch (Exception e) {
            if (file.exists()) {
                return file.delete();
            }
        }

        try {
            return file.delete();
        } catch (Exception e) {
            return false;
        }
    }

    private static void failIfNullBytePresent(String path) {
        int len = path.length();
        for (int i = 0; i < len; i++) {
            if (path.charAt(i) == 0) {
                throw new IllegalArgumentException("Null byte present in file/path name.");
            }
        }
    }

    private static void forceDelete(final File file) throws IOException {
        if (file.isDirectory()) {
            deleteDirectory(file);
        } else {
            final boolean filePresent = file.exists();
            if (!file.delete()) {
                if (!filePresent) {
                    throw new FileNotFoundException("File does not exist: " + file);
                }
                final String message = "Unable to delete file: " + file;
                throw new IOException(message);
            }
        }
    }

    /**
     * Gets the base name, minus the full path and extension, from a full filename.
     * <p>
     * This method will handle a file in either Unix or Windows format.
     * The text after the last forward or backslash and before the last dot is returned.
     * <pre>
     * a/b/c.txt --&gt; c
     * a.txt     --&gt; a
     * a/b/c     --&gt; c
     * a/b/c/    --&gt; ""
     * </pre>
     * <p>
     * The output will be the same irrespective of the machine that the code is running on.
     *
     * @param filename  the filename to query, null returns null
     * @return the name of the file without the path, or an empty string if none exists. Null bytes inside string
     * will be removed
     */
    public static String getBaseName(final String filename) {
        return removeExtension(getName(filename));
    }

    public static String getName(final String filename) {
        if (filename == null) {
            return null;
        }
        failIfNullBytePresent(filename);
        final int index = indexOfLastSeparator(filename);
        return filename.substring(index + 1);
    }

    public static String getExtension(File file) {
        if (file == null) {
            return null;
        }
        final String filename = file.getName();
        return getExtension(filename);
    }

    public static String getExtension(final String filename) {
        if (filename == null) {
            return null;
        }
        final int index = indexOfExtension(filename);
        if (index == -1) {
            return "";
        } else {
            return filename.substring(index + 1);
        }
    }

    public static String getRealFileExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int index = fileName.lastIndexOf('.');
        if (index == -1) {
            return "";
        }
        return fileName.substring(index, fileName.length()).toLowerCase().replaceAll("\\(\\d*\\)", "").replaceAll("-\\d+", "").replaceAll("-\\s?copy", "").replaceAll("\\scopy(\\s\\d+)?", "").trim();
    }

    public static int indexOfExtension(final String filename) {
        if (filename == null) {
            return -1;
        }
        final int extensionPos = filename.lastIndexOf('.');
        final int lastSeparator = indexOfLastSeparator(filename);
        return lastSeparator > extensionPos ? -1 : extensionPos;
    }

    public static int indexOfLastSeparator(final String filename) {
        if (filename == null) {
            return -1;
        }
        final int lastUnixPos = filename.lastIndexOf('/');
        final int lastWindowsPos = filename.lastIndexOf('\\');
        return Math.max(lastUnixPos, lastWindowsPos);
    }

    public static File[] listFiles(File directory) {
        if (!directory.exists()) {
            final String message = directory + " does not exist";
            throw new IllegalArgumentException(message);
        }
        if (!directory.isDirectory()) {
            final String message = directory + " is not a directory";
            throw new IllegalArgumentException(message);
        }
        return directory.listFiles();
    }

    public static void mkdir(final File directory) throws IOException {
        if (directory.exists() && !directory.isDirectory()) {
            final String message = "File " + directory + " exists and is not a directory. Unable to create directory.";
            throw new IOException(message);
        } else {
            if (!directory.mkdirs() && !directory.isDirectory()) {
                final String message = "Unable to create directory " + directory;
                throw new IOException(message);
            }
        }
    }

    public static void mkdirParent(final File file) throws IOException {
        final File parent = file.getParentFile();
        if (parent == null) {
            return;
        }
        mkdir(parent);
    }

    /**
     * Removes the extension from a filename.
     * <p>
     * This method returns the textual part of the filename before the last dot.
     * There must be no directory separator after the dot.
     * <pre>
     * foo.txt    --&gt; foo
     * a\b\c.jpg  --&gt; a\b\c
     * a\b\c      --&gt; a\b\c
     * a.b\c      --&gt; a.b\c
     * </pre>
     * <p>
     * The output will be the same irrespective of the machine that the code is running on.
     *
     * @param filename  the filename to query, null returns null
     * @return the filename minus the extension
     */
    public static String removeExtension(final String filename) {
        if (filename == null) {
            return null;
        }
        failIfNullBytePresent(filename);

        final int index = indexOfExtension(filename);
        if (index == -1) {
            return filename;
        } else {
            return filename.substring(0, index);
        }
    }

    public static boolean setOwnerFullAccess(Path path) throws IOException {
        FileSystem fileSystem = path.getFileSystem();
        Set<String> fileAttributeViews = fileSystem.supportedFileAttributeViews();
        FileOwnerAttributeView ownerAttributeView = Files.getFileAttributeView(path, FileOwnerAttributeView.class);
        final UserPrincipal owner = ownerAttributeView.getOwner();
        if (fileAttributeViews.contains("acl")) {
            final Set<AclEntryPermission> permissions = EnumSet.allOf(AclEntryPermission.class);
            AclFileAttributeView fileAttributeView = Files.getFileAttributeView(path, AclFileAttributeView.class);
            Builder builder = AclEntry.newBuilder();
            builder.setPrincipal(owner);
            builder.setPermissions(permissions);
            builder.setType(AclEntryType.ALLOW);
            fileAttributeView.setAcl(Collections.singletonList(builder.build()));
            return true;
        } else if (fileAttributeViews.contains("posix")) {
            final Set<PosixFilePermission> permissions = EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE);
            PosixFileAttributeView fileAttributeView = Files.getFileAttributeView(path, PosixFileAttributeView.class);
            fileAttributeView.setPermissions(permissions);
            return true;
        }
        return false;
    }

    private FileUtils() {
    }
}
