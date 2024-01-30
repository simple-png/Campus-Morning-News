package com.heima.utils.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.*;

public class ZipUtils {

    /**
     * 使用gzip进行压缩
     */
    public static String gzip(String primStr) {
        if (primStr == null || primStr.length() == 0) {
            return primStr;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(primStr.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    /**
     * 使用gzip进行解压缩
     */
    public static String gunzip(String compressedStr) {
        if (compressedStr == null) {
            return null;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(Base64.getDecoder().decode(compressedStr));
        try (GZIPInputStream ginzip = new GZIPInputStream(in)) {
            byte[] buffer = new byte[1024];
            int offset = -1;
            while ((offset = ginzip.read(buffer)) != -1) {
                out.write(buffer, 0, offset);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return out.toString();
    }

    /**
     * 使用zip进行压缩
     */
    public static String zip(String str) {
        if (str == null) {
            return null;
        }

        byte[] compressed;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zout = new ZipOutputStream(out)) {
            zout.putNextEntry(new ZipEntry("0"));
            zout.write(str.getBytes());
            zout.closeEntry();
            compressed = out.toByteArray();
        } catch (IOException e) {
            compressed = null;
            e.printStackTrace();
        }

        return Base64.getEncoder().encodeToString(compressed);
    }

    /**
     * 使用zip进行解压缩
     */
    public static String unzip(String compressedStr) {
        if (compressedStr == null) {
            return null;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(Base64.getDecoder().decode(compressedStr));
        try (ZipInputStream zin = new ZipInputStream(in)) {
            zin.getNextEntry();
            byte[] buffer = new byte[1024];
            int offset = -1;
            while ((offset = zin.read(buffer)) != -1) {
                out.write(buffer, 0, offset);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return out.toString();
    }
}
