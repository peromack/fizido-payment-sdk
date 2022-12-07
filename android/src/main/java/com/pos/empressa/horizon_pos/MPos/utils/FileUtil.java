package com.pos.empressa.horizon_pos.MPos.utils;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 文件读写工具类
 *
 * @author jack
 */
public class FileUtil {

    public static void makeFileDir(String fileDir) {
        File file = new File(fileDir);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    public static void deleteFile(String file) {
        File f = new File(file);
        if (f.exists()) {
            f.delete();
        }
    }

    /**
     * 如果文件不存在，就创建文件
     *
     * @param path 文件路径
     * @return
     */
    public static String createFile(String path) {
        File file = new File(path);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        return path;
    }

    /**
     * 向文件中写入数据
     *
     * @param filePath 目标文件全路径
     * @param data     要写入的数据
     * @return true表示写入成功  false表示写入失败
     */
    public static boolean writeBytes(String filePath, byte[] data) {
        try {
            createFile(filePath);
            FileOutputStream fos = new FileOutputStream(filePath);
            fos.write(data);
            fos.close();
            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return false;
    }

    /**
     * 从文件中读取数据
     *
     * @param file
     * @return
     */
    public static byte[] readBytes(String file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            int len = fis.available();
            byte[] buffer = new byte[len];
            fis.read(buffer);
            fis.close();
            return buffer;
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return null;

    }

    /**
     * 向文件中写入字符串String类型的内容
     *
     * @param file    文件路径
     * @param content 文件内容
     * @param charset 写入时候所使用的字符集
     */
    public static void writeString(String file, String content, String charset) {
        try {
            byte[] data = content.getBytes(charset);
            writeBytes(file, data);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * 从文件中读取数据，返回类型是字符串String类型
     *
     * @param file    文件路径
     * @param charset 读取文件时使用的字符集，如utf-8、GBK等
     * @return
     */
    public static String readString(String file, String charset) {
        byte[] data = readBytes(file);
        String ret = null;

        try {
            ret = new String(data, charset);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return ret;
    }

    public static int copyfile(String fromFile, String toFile) {
        File file = new File(toFile);
        try {
            if (file.exists()) {
                file.delete();
            }
            InputStream from = new FileInputStream(fromFile);
            OutputStream to = new FileOutputStream(toFile);

            byte bt[] = new byte[1024];
            int c;
            while ((c = from.read(bt)) > 0) {
                to.write(bt, 0, c);
            }
            from.close();
            to.close();
            return 0;
        } catch (Exception ex) {
            ex.printStackTrace();
            file.delete();
            return -1;
        }
    }

    /**
     * 专为Android4.4设计的从Uri获取文件绝对路径，以前的方法已不好使
     */
    @SuppressLint("NewApi")
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}