package com.dyrnq.utils;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.IOUtils;

import java.io.*;


public class TarUtils {
    /**
     * 将目录压缩成一个tar.gz文件
     *
     * @param sourceDirPath
     * @param targetFilePath
     * @throws IOException
     */
    public static void tarGz(String sourceDirPath, String targetFilePath) throws IOException {
        FileOutputStream fileOutputStream = null;
        BufferedOutputStream bufferedOutputStream = null;
        GzipCompressorOutputStream gzipOutputStream = null;
        TarArchiveOutputStream tarArchiveOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(targetFilePath);
            bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
            gzipOutputStream = new GzipCompressorOutputStream(bufferedOutputStream);
            tarArchiveOutputStream = new TarArchiveOutputStream(gzipOutputStream);
            File sourceDir = new File(sourceDirPath);
            for (File file : sourceDir.listFiles()) {
                addFileToTarGz(tarArchiveOutputStream, "", file);
            }
        } finally {
            IOUtils.closeQuietly(tarArchiveOutputStream, null);
            IOUtils.closeQuietly(gzipOutputStream, null);
            IOUtils.closeQuietly(bufferedOutputStream, null);
            IOUtils.closeQuietly(fileOutputStream, null);
        }
    }

    private static void addFileToTarGz(TarArchiveOutputStream tarArchiveOutputStream, String base, File file) throws IOException {
        String entryName = base + file.getName();
        TarArchiveEntry tarEntry = new TarArchiveEntry(file, entryName);
        tarArchiveOutputStream.putArchiveEntry(tarEntry);

        if (file.isFile()) {
            try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
                byte[] buffer = new byte[1024];
                int count;
                while ((count = inputStream.read(buffer)) != -1) {
                    tarArchiveOutputStream.write(buffer, 0, count);
                }
            }
            tarArchiveOutputStream.closeArchiveEntry();
        } else if (file.isDirectory()) {
            tarArchiveOutputStream.closeArchiveEntry();
            for (File childFile : file.listFiles()) {
                String childBase = entryName + "/";
                addFileToTarGz(tarArchiveOutputStream, childBase, childFile);
            }
        }
    }


    /**
     * 解压缩tar.gz文件
     *
     * @param tarGzFilepath
     * @param destDirectory
     * @throws IOException
     */
    public static void extractTarGz(String tarGzFilepath, String destDirectory) throws IOException {
        FileInputStream fis = new FileInputStream(tarGzFilepath);
        GzipCompressorInputStream gzis = new GzipCompressorInputStream(fis);
        TarArchiveInputStream tais = new TarArchiveInputStream(gzis);

        TarArchiveEntry entry;
        while ((entry = tais.getNextTarEntry()) != null) {
            String fileName = entry.getName();
            if (fileName.contains("..")) {
                // 跳过包含..的文件名
                continue;
            }
            File outputFile = new File(destDirectory, fileName);
            if (entry.isDirectory()) {
                // 如果是目录，则创建目录
                outputFile.mkdirs();
            } else {
                // 如果是文件，则写入文件内容
                byte[] buffer = new byte[4096];
                int bytesRead;
                FileOutputStream fos = new FileOutputStream(outputFile);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                while ((bytesRead = tais.read(buffer)) != -1) {
                    bos.write(buffer, 0, bytesRead);
                }
                bos.close();
            }
        }
        tais.close();
    }

}
