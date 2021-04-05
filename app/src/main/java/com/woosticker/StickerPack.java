package com.woosticker;

import java.io.File;
import java.io.FileFilter;

/**
 * Helper class to provide pack-related information
 * A "Pack" is informally represented as a File
 */
public class StickerPack {
    private File[] stickers;
    public StickerPack(File packDir){
        // Need to filter out directories because the base directory has to have other packs'
        // directories.
        this.stickers = packDir.listFiles(new FileFilter() {
            @Override public boolean accept(File file) {
                return file.isFile();
            }
        });
    }

    /**
     * Provides a sticker to use as the pack-nav container thumbnail.
     * Currently just takes the first element, but could theoretically include any selection logic.
     *
     * @return File that should be used for thumbnail
     */
    public File getThumbSticker(){
        return (File) this.stickers[0];
    }

    /**
     * Note: When MainActivity copies files over, it filters out all non-supported files (i.e. any
     * file that is not supported as well as directories). Because of this there is no extra filter
     * in this function. The exception is the base directory, which is handled in the constructor.
     *
     * @return Array of Files corresponding to all stickers found in this pack
     */
    public File[] getStickerList(){
        return (File[]) this.stickers;
    }
}
