package com.hb.cleaner;

import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class Cleaner {

    static final class Scan {
        final List<File> cache = new ArrayList<>();
        final List<File> thumbs = new ArrayList<>();
        final List<File> temp = new ArrayList<>();
        long cacheSize, thumbSize, tempSize;

        long total() {
            return cacheSize + thumbSize + tempSize;
        }

        int count() {
            return cache.size() + thumbs.size() + temp.size();
        }
    }

    static Scan scan() {
        Scan s = new Scan();
        File root = Environment.getExternalStorageDirectory();

        // 1) Onno app er external cache: Android/data/<pkg>/cache
        File data = new File(root, "Android/data");
        File[] pkgs = data.listFiles();
        if (pkgs != null) {
            for (File pkg : pkgs) {
                File c = new File(pkg, "cache");
                if (c.isDirectory()) {
                    s.cacheSize += collect(c, s.cache, 0);
                }
            }
        }

        // 2) Gallery thumbnail cache
        String[] thumbDirs = {"DCIM/.thumbnails", "Pictures/.thumbnails", ".thumbnails"};
        for (String d : thumbDirs) {
            File t = new File(root, d);
            if (t.isDirectory()) {
                s.thumbSize += collect(t, s.thumbs, 0);
            }
        }

        // 3) .tmp / .temp / .log file
        walkTemp(root, 0, s);
        return s;
    }

    private static long collect(File dir, List<File> out, int depth) {
        if (depth > 12) return 0;
        File[] list = dir.listFiles();
        if (list == null) return 0;
        long size = 0;
        for (File f : list) {
            if (f.isDirectory()) {
                size += collect(f, out, depth + 1);
            } else {
                out.add(f);
                size += f.length();
            }
        }
        return size;
    }

    private static void walkTemp(File dir, int depth, Scan s) {
        if (depth > 8) return;
        File[] list = dir.listFiles();
        if (list == null) return;
        for (File f : list) {
            String name = f.getName();
            if (f.isDirectory()) {
                if (depth == 0 && name.equals("Android")) continue;
                if (name.equals(".thumbnails")) continue;
                walkTemp(f, depth + 1, s);
            } else {
                String n = name.toLowerCase(Locale.ROOT);
                if (n.endsWith(".tmp") || n.endsWith(".temp") || n.endsWith(".log")
                        || n.startsWith(".thumbdata")) {
                    s.temp.add(f);
                    s.tempSize += f.length();
                }
            }
        }
    }

    /** Dewa file gulo delete kore, koto byte muchlo seta ferot dey. */
    static long delete(List<File> files) {
        long freed = 0;
        for (File f : files) {
            long len = f.length();
            if (f.delete()) freed += len;
        }
        return freed;
    }

    /** Folder er bhitorer sob muche, folder nijer thake. */
    static long clearDir(File dir) {
        if (dir == null) return 0;
        File[] list = dir.listFiles();
        if (list == null) return 0;
        long freed = 0;
        for (File f : list) {
            if (f.isDirectory()) {
                freed += clearDir(f);
                f.delete();
            } else {
                long len = f.length();
                if (f.delete()) freed += len;
            }
        }
        return freed;
    }
}
