package com.woosticker;

import androidx.annotation.NonNull;

import java.io.File;

/**
 * Helper class to provide pack-related information
 * A "Pack" is informally represented as a File
 */
public class StickerPack {
    private final File[] stickers;

    public StickerPack(@NonNull File packDir) {
        // Need to filter out directories because the base directory has to have other packs'
        // directories.
        stickers = packDir.listFiles(File::isFile);
    }

    /**
     * Note: When MainActivity copies files over, it filters out all non-supported files (i.e. any
     * file that is not supported as well as directories). Because of this there is no extra filter
     * in this function. The exception is the base directory, which is handled in the constructor.
     *
     * @return Array of Files corresponding to all stickers found in this pack
     */
    @NonNull
    public File[] getStickerList() {
        return stickers;
    }

    /**
     * Provides a sticker to use as the pack-nav container thumbnail.
     * Currently just takes the first element, but could theoretically include any selection logic.
     *
     * @return File that should be used for thumbnail
     */
    @NonNull
    public File getThumbSticker() {
        return stickers[0];
    }
}
