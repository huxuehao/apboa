package com.hxh.apboa.common.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 描述：文件夹工具类，提供递归创建、删除、复制、移动等常用文件夹操作
 *
 * @author huxuehao
 **/
public class FolderUtils {

    /**
     * 基于相对路径递归创建文件夹，相对于系统属性 user.dir
     *
     * @param relativePath 相对路径
     * @return 创建后的文件夹对象
     */
    public static File mkdirsByRelativePath(String relativePath) {
        if (FuncUtils.isEmpty(relativePath)) {
            throw new IllegalArgumentException("相对路径不能为空");
        }
        String basePath = System.getProperty("user.dir");
        Path fullPath = Paths.get(basePath, relativePath);
        return mkdirs(fullPath);
    }

    /**
     * 基于相对路径递归创建文件夹，相对于指定的基础路径
     *
     * @param basePath     基础路径
     * @param relativePath 相对路径
     * @return 创建后的文件夹对象
     */
    public static File mkdirsByRelativePath(String basePath, String relativePath) {
        if (FuncUtils.isEmpty(basePath)) {
            throw new IllegalArgumentException("基础路径不能为空");
        }
        if (FuncUtils.isEmpty(relativePath)) {
            throw new IllegalArgumentException("相对路径不能为空");
        }
        Path fullPath = Paths.get(basePath, relativePath);
        return mkdirs(fullPath);
    }

    /**
     * 基于绝对路径递归创建文件夹
     *
     * @param absolutePath 绝对路径
     * @return 创建后的文件夹对象
     */
    public static File mkdirsByAbsolutePath(String absolutePath) {
        if (FuncUtils.isEmpty(absolutePath)) {
            throw new IllegalArgumentException("绝对路径不能为空");
        }
        Path path = Paths.get(absolutePath);
        if (!path.isAbsolute()) {
            throw new IllegalArgumentException("路径不是绝对路径: " + absolutePath);
        }
        return mkdirs(path);
    }

    /**
     * 递归创建文件夹
     *
     * @param path 文件夹路径
     * @return 创建后的文件夹对象
     */
    private static File mkdirs(Path path) {
        File folder = path.toFile();
        if (!folder.exists()) {
            boolean created = folder.mkdirs();
            if (!created && !folder.exists()) {
                throw new RuntimeException("创建文件夹失败: " + path);
            }
        }
        if (folder.exists() && !folder.isDirectory()) {
            throw new RuntimeException("路径已存在但不是文件夹: " + path);
        }
        return folder;
    }

    /**
     * 判断文件夹是否存在
     *
     * @param path 路径
     * @return 存在返回true，否则返回false
     */
    public static boolean exists(String path) {
        if (FuncUtils.isEmpty(path)) {
            return false;
        }
        File folder = new File(path);
        return folder.exists() && folder.isDirectory();
    }

    /**
     * 判断文件夹是否为空（不存在或目录下无任何文件和子目录）
     *
     * @param path 路径
     * @return 为空返回true，否则返回false
     */
    public static boolean isEmpty(String path) {
        if (FuncUtils.isEmpty(path)) {
            return true;
        }
        File folder = new File(path);
        if (!folder.exists() || !folder.isDirectory()) {
            return true;
        }
        String[] files = folder.list();
        return files == null || files.length == 0;
    }

    /**
     * 递归删除文件夹及其所有内容
     *
     * @param path 文件夹路径
     * @return 删除成功返回true，否则返回false
     */
    public static boolean deleteRecursively(String path) {
        if (FuncUtils.isEmpty(path)) {
            return false;
        }
        File folder = new File(path);
        return deleteRecursively(folder);
    }

    /**
     * 递归删除文件夹及其所有内容
     *
     * @param folder 文件夹对象
     * @return 删除成功返回true，否则返回false
     */
    public static boolean deleteRecursively(File folder) {
        if (folder == null || !folder.exists()) {
            return false;
        }
        if (!folder.isDirectory()) {
            return folder.delete();
        }
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                deleteRecursively(file);
            }
        }
        return folder.delete();
    }

    /**
     * 递归复制文件夹
     *
     * @param srcPath  源文件夹路径
     * @param destPath 目标文件夹路径
     * @throws IOException 复制失败时抛出
     */
    public static void copyRecursively(String srcPath, String destPath) throws IOException {
        if (FuncUtils.isEmpty(srcPath)) {
            throw new IllegalArgumentException("源路径不能为空");
        }
        if (FuncUtils.isEmpty(destPath)) {
            throw new IllegalArgumentException("目标路径不能为空");
        }
        Path src = Paths.get(srcPath);
        Path dest = Paths.get(destPath);
        copyRecursively(src, dest);
    }

    /**
     * 递归复制文件夹
     *
     * @param src  源文件夹路径
     * @param dest 目标文件夹路径
     * @throws IOException 复制失败时抛出
     */
    private static void copyRecursively(Path src, Path dest) throws IOException {
        if (!Files.exists(src) || !Files.isDirectory(src)) {
            throw new IllegalArgumentException("源路径不存在或不是文件夹: " + src);
        }
        // 使用 Files.walkFileTree 递归复制
        Files.walkFileTree(src, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDir = dest.resolve(src.relativize(dir));
                Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, dest.resolve(src.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * 移动文件夹（递归移动源文件夹内容到目标路径）
     *
     * @param srcPath  源文件夹路径
     * @param destPath 目标文件夹路径
     * @throws IOException 移动失败时抛出
     */
    public static void move(String srcPath, String destPath) throws IOException {
        if (FuncUtils.isEmpty(srcPath)) {
            throw new IllegalArgumentException("源路径不能为空");
        }
        if (FuncUtils.isEmpty(destPath)) {
            throw new IllegalArgumentException("目标路径不能为空");
        }
        Path src = Paths.get(srcPath);
        Path dest = Paths.get(destPath);
        if (!Files.exists(src) || !Files.isDirectory(src)) {
            throw new IllegalArgumentException("源路径不存在或不是文件夹: " + src);
        }
        // 确保目标父目录存在
        mkdirsByAbsolutePath(dest.getParent().toString());
        Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * 获取文件夹大小（字节）
     *
     * @param path 文件夹路径
     * @return 文件夹总大小（字节），路径无效时返回0
     */
    public static long getSize(String path) {
        if (FuncUtils.isEmpty(path)) {
            return 0;
        }
        File folder = new File(path);
        return getSize(folder);
    }

    /**
     * 获取文件夹大小（字节）
     *
     * @param folder 文件夹对象
     * @return 文件夹总大小（字节），路径无效时返回0
     */
    public static long getSize(File folder) {
        if (folder == null || !folder.exists()) {
            return 0;
        }
        if (folder.isFile()) {
            return folder.length();
        }
        long size = 0;
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                size += getSize(file);
            }
        }
        return size;
    }

    /**
     * 获取格式化的文件夹大小
     *
     * @param path 文件夹路径
     * @return 格式化后的大小字符串，如 "1.5 GB"、"256.3 MB"
     */
    public static String getReadableSize(String path) {
        return formatFileSize(getSize(path));
    }

    /**
     * 列出文件夹下的直接子文件夹
     *
     * @param path 文件夹路径
     * @return 子文件夹列表，路径无效时返回空列表
     */
    public static List<File> listSubFolders(String path) {
        if (FuncUtils.isEmpty(path)) {
            return Collections.emptyList();
        }
        File folder = new File(path);
        if (!folder.exists() || !folder.isDirectory()) {
            return Collections.emptyList();
        }
        File[] files = folder.listFiles(File::isDirectory);
        if (files == null) {
            return Collections.emptyList();
        }
        List<File> result = new ArrayList<>(files.length);
        Collections.addAll(result, files);
        return result;
    }

    /**
     * 列出文件夹下的直接文件（非目录）
     *
     * @param path 文件夹路径
     * @return 文件列表，路径无效时返回空列表
     */
    public static List<File> listFiles(String path) {
        if (FuncUtils.isEmpty(path)) {
            return Collections.emptyList();
        }
        File folder = new File(path);
        if (!folder.exists() || !folder.isDirectory()) {
            return Collections.emptyList();
        }
        File[] files = folder.listFiles(File::isFile);
        if (files == null) {
            return Collections.emptyList();
        }
        List<File> result = new ArrayList<>(files.length);
        Collections.addAll(result, files);
        return result;
    }

    /**
     * 递归列出文件夹下所有文件（包含子目录中的文件）
     *
     * @param path 文件夹路径
     * @return 所有文件列表，路径无效时返回空列表
     */
    public static List<File> listAllFiles(String path) {
        if (FuncUtils.isEmpty(path)) {
            return Collections.emptyList();
        }
        File folder = new File(path);
        if (!folder.exists() || !folder.isDirectory()) {
            return Collections.emptyList();
        }
        List<File> result = new ArrayList<>();
        collectAllFiles(folder, result);
        return result;
    }

    /**
     * 递归收集文件夹下所有文件
     *
     * @param folder 文件夹对象
     * @param result 结果收集列表
     */
    private static void collectAllFiles(File folder, List<File> result) {
        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                collectAllFiles(file, result);
            } else {
                result.add(file);
            }
        }
    }

    /**
     * 重命名文件夹
     *
     * @param path    文件夹路径
     * @param newName 新名称
     * @return 重命名后的文件夹对象
     */
    public static File rename(String path, String newName) {
        if (FuncUtils.isEmpty(path) || FuncUtils.isEmpty(newName)) {
            throw new IllegalArgumentException("路径和新名称不能为空");
        }
        File folder = new File(path);
        if (!folder.exists() || !folder.isDirectory()) {
            throw new IllegalArgumentException("路径不存在或不是文件夹: " + path);
        }
        File newFolder = new File(folder.getParent(), newName);
        if (!folder.renameTo(newFolder)) {
            throw new RuntimeException("重命名文件夹失败: " + path + " -> " + newName);
        }
        return newFolder;
    }

    /**
     * 获取文件夹下的文件数量（仅直接子项，不含子目录内容）
     *
     * @param path 文件夹路径
     * @return 文件数量，路径无效时返回0
     */
    public static int countFiles(String path) {
        return listFiles(path).size();
    }

    /**
     * 获取文件夹下的子文件夹数量（仅直接子目录）
     *
     * @param path 文件夹路径
     * @return 子文件夹数量，路径无效时返回0
     */
    public static int countSubFolders(String path) {
        return listSubFolders(path).size();
    }

    /**
     * 格式化文件大小为可读字符串
     *
     * @param size 字节数
     * @return 格式化后的字符串
     */
    private static String formatFileSize(long size) {
        if (size < 0) {
            return "0 B";
        }
        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double fileSize = size;
        while (fileSize >= 1024 && unitIndex < units.length - 1) {
            fileSize /= 1024;
            unitIndex++;
        }
        return String.format("%.1f %s", fileSize, units[unitIndex]);
    }
}
