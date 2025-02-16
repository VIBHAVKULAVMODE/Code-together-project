package com.codeTogether.util;

import java.io.ByteArrayOutputStream;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class FileUtils {

    public static byte[] compressFile(byte[] data) {
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        deflater.setInput(data);
        deflater.finish();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
        byte[] buffer = new byte[4096];
        while (!deflater.finished()) {
            int size = deflater.deflate(buffer);
            outputStream.write(buffer, 0, size);
        }
        return outputStream.toByteArray();
    }

    public static byte[] decompressFile(byte[] data) {
        Inflater inflater = new Inflater();
        inflater.setInput(data);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
        byte[] buffer = new byte[4096];
        try {
            while (!inflater.finished()) {
                int size = inflater.inflate(buffer);
                outputStream.write(buffer, 0, size);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error decompressing file", e);
        }
        return outputStream.toByteArray();
    }
}
