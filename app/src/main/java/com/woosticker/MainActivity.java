package com.woosticker;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends Activity {
    private final int CHOOSE_STICKER_DIR = 62519;
    private SharedPreferences sharedPref = null;
    private final HashMap<String, String> SUPPORTED_MIMES = Utils.get_supported_mimes();

    /**
     * Restart the application. See usage comment above.
     *
     * @param context
     */
    //https://stackoverflow.com/a/46848226
    private static void triggerRebirth(Context context) {
        PackageManager packageManager = context.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(context.getPackageName());
        ComponentName componentName = intent.getComponent();
        Intent mainIntent = Intent.makeRestartActivityTask(componentName);
        context.startActivity(mainIntent);
        Runtime.getRuntime().exit(0);
    }

    /**
     * Sets up content view, shared prefs, etc.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        setContentView(R.layout.activity_main);
        refreshStickerDirPath();
    }

    /**
     * Rereads saved sticker dir path from preferences
     */
    private void refreshStickerDirPath() {
        String stickerDirPath = sharedPref.getString("stickerDirPath", "none set");

        TextView dirStatus = findViewById(R.id.stickerDirStatus);
        dirStatus.setText(stickerDirPath);
    }

    /**
     * Handles ACTION_OPEN_DOCUMENT_TREE result and adds the returned Uri to shared prefs
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHOOSE_STICKER_DIR && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("stickerDirPath", data.getData().toString());
                editor.apply();
                refreshStickerDirPath();
            }
        }
    }

    /**
     * Called on button press to choose a new directory
     *
     * @param view
     */
    public void chooseDir(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, CHOOSE_STICKER_DIR);
    }

    /**
     * Copies images from pack directory by calling importSticker() on all of them
     *
     * @param pack source pack
     */
    private void importPack(DocumentFile pack) {
        DocumentFile[] stickers = pack.listFiles();
        for (int i = 0; i < stickers.length; i++) {
            importSticker(stickers[i], pack.getName() + "/");
        }
    }

    /**
     * For each sticker, check if it is in a compatible file format with woosticker
     *
     * @param sticker sticker to check compatability with woosticker for
     * @return true if supported image type
     */
    private boolean canImportSticker(DocumentFile sticker) {
        ArrayList<String> mimesToCheck = new ArrayList<String>(SUPPORTED_MIMES.keySet());
        return !(sticker.isDirectory() ||
                !mimesToCheck.contains(Utils.getFileExtension(sticker.getName())));
    }

    /**
     * Copies stickers from source to internal storage
     *
     * @param sticker sticker to copy over
     * @param pack    the pack which the sticker belongs to
     */
    private void importSticker(DocumentFile sticker, String pack) {
        if (!canImportSticker(sticker)) {
            return;
        }
        //this path business is a little icky....
        File destSticker = new File(getFilesDir(), "stickers" + "/" + pack + sticker.getName());
        destSticker.getParentFile().mkdirs();
        try {
            InputStream is = getContentResolver().openInputStream(sticker.getUri());
            Files.copy(is, destSticker.toPath());
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Called on button press to import stickers from source directory to internal storage
     *
     * @param view
     */
    public void importStickers(View view) {
        File oldStickers = new File(getFilesDir(), "stickers");
        deleteRecursive(oldStickers);
        String stickerDirPath = sharedPref.getString("stickerDirPath", "none set");
        if (!stickerDirPath.equals("none set")) {
            DocumentFile tree = DocumentFile.fromTreeUri(this, Uri.parse(stickerDirPath));

            DocumentFile[] files = tree.listFiles();
            File destinationDir = new File(getFilesDir(), "stickers");

            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile())
                    importSticker(files[i], "");
                if (files[i].isDirectory())
                    importPack(files[i]);
            }
        }

        //Inelegant, but the easiest way I could guarantee the pack/image reloading to be triggered
        //when an import is performed. Maybe should just decouple it from onCreate()....
        triggerRebirth(this);
    }

    /**
     * Delete everything from input File
     *
     * @param fileOrDirectory File to start deleting from
     */
    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }
        fileOrDirectory.delete();
    }
}
