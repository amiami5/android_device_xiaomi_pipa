/*
 * Copyright (C) 2026 nullpointer1101
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.pipacontrols.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public final class FileUtils {

    private static final String TAG = "FileUtils";

    private FileUtils() {}

    public static boolean fileExists(String path) {
        return new File(path).exists();
    }

    public static String readOneLine(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            return br.readLine();
        } catch (IOException e) {
            Log.e(TAG, "readOneLine failed: " + path, e);
            return null;
        }
    }

    public static boolean writeLine(String path, String value) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(path))) {
            bw.write(value + "\n");
            return true;
        } catch (IOException e) {
            Log.e(TAG, "writeLine failed: " + path, e);
            return false;
        }
    }
}
