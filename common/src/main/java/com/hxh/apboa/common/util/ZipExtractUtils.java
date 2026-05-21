package com.hxh.apboa.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * ZIP 安全解压工具，支持 UTF-8 与 GBK 文件名编码（兼容 Windows 中文路径）。
 */
public final class ZipExtractUtils {

    private static final Charset GBK = Charset.forName("GBK");

    private ZipExtractUtils() {
    }

    /**
     * 将 ZIP 解压到目标目录，自动尝试 UTF-8 与 GBK 编码。
     *
     * @param zipFile   压缩包路径
     * @param targetDir 解压目标目录
     */
    public static void extractZipSafely(Path zipFile, Path targetDir) throws IOException {
        Files.createDirectories(targetDir);
        try {
            extractZipWithCharset(zipFile, targetDir, StandardCharsets.UTF_8);
        } catch (ZipEntryCharsetException e) {
            resetTargetDir(targetDir);
            extractZipWithCharset(zipFile, targetDir, GBK);
        }
    }

    /**
     * 将输入流中的 ZIP 解压到目标目录（会先写入临时文件以支持编码回退）。
     *
     * @param zipInput    压缩包输入流
     * @param targetDir   解压目标目录
     * @param tempZipFile 临时 zip 文件路径
     */
    public static void extractZipSafely(InputStream zipInput, Path targetDir, Path tempZipFile) throws IOException {
        Files.createDirectories(targetDir);
        Files.createDirectories(tempZipFile.getParent());
        Files.copy(zipInput, tempZipFile, StandardCopyOption.REPLACE_EXISTING);
        try {
            extractZipSafely(tempZipFile, targetDir);
        } finally {
            Files.deleteIfExists(tempZipFile);
        }
    }

    private static void extractZipWithCharset(Path zipFile, Path targetDir, Charset charset) throws IOException {
        try (InputStream fis = Files.newInputStream(zipFile);
             ZipInputStream zis = new ZipInputStream(fis, charset)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                extractEntry(zis, entry, targetDir);
                zis.closeEntry();
            }
        } catch (IllegalArgumentException e) {
            if (isMalformedZipEntryName(e)) {
                throw new ZipEntryCharsetException(e);
            }
            throw e;
        }
    }

    private static void extractEntry(ZipInputStream zis, ZipEntry entry, Path targetDir) throws IOException {
        Path entryPath = targetDir.resolve(entry.getName()).normalize();
        if (!entryPath.startsWith(targetDir)) {
            throw new IOException("非法的压缩包路径: " + entry.getName());
        }
        if (entry.isDirectory()) {
            Files.createDirectories(entryPath);
            return;
        }
        Path parent = entryPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void resetTargetDir(Path targetDir) throws IOException {
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
            return;
        }
        try (Stream<Path> walk = Files.walk(targetDir)) {
            walk.sorted(Comparator.reverseOrder())
                    .filter(path -> !path.equals(targetDir))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            throw new IllegalStateException("清理解压目录失败: " + path, e);
                        }
                    });
        }
    }

    private static boolean isMalformedZipEntryName(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof MalformedInputException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.contains("malformed input")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static final class ZipEntryCharsetException extends IOException {
        private ZipEntryCharsetException(Throwable cause) {
            super("ZIP entry name charset mismatch", cause);
        }
    }
}
