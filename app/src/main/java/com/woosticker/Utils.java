package com.woosticker;

import androidx.annotation.NonNull;

import java.util.HashMap;

/**
 * Class to provide utils that are shared across woosticker.
 */
public final class Utils {
    /**
     * @param name the File's name. Takes in a string here instead of a File because in certain
     *             places we have to use DocumentFile instead-- String name can be found by calling
     *             .getName() on both, but they are different classes.
     * @return returns "." inclusive file extension.
     */
    @NonNull
    public static String getFileExtension(@NonNull String name) {
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return "";
        }
        return name.substring(lastIndexOf);
    }

    /**
     * Needs to create a new HashMap on every call because shallow copies will cause issues between
     * different input areas that support different media types.
     *
     * @return HashMap of woosticker-supported mimes. Keys are "." inclusive.
     */
    @NonNull
    public static HashMap<String, String> get_supported_mimes() {
        return new HashMap<String, String>() {{
            put(".gif", "image/gif");
            put(".png", "image/png");
            put(".apng", "image/png");
            put(".jpg", "image/jpg");
            put(".webp", "image/webp");
        }};
    }
}
