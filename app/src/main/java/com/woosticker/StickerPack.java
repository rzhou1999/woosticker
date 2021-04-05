package com.woosticker;

import java.io.File;
import java.io.FileFilter;

public class StickerPack {
    private File INTERNAL_DIR;

    private String packName;
    private File[] stickers;
    public StickerPack(File packDir){
        this.packName = packName;

        this.stickers = packDir.listFiles(new FileFilter() {
            @Override public boolean accept(File file) {
                return file.isFile();
            }
        });
    }

    public File getThumbSticker(){
        return (File) this.stickers[0];
    }

    public String getName(){
        return this.packName;
    }

    public File[] getStickerList(){
        return (File[]) this.stickers;
    }
}
