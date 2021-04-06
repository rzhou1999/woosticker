package com.woosticker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class MainActivity extends Activity {
    private final int CHOOSE_STICKER_DIR = 62519;
    private final HashMap<String, String> SUPPORTED_MIMES = Utils.get_supported_mimes();
    private SharedPreferences sharedPref = null;

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

    /**
     * Copies images from pack directory by calling importSticker() on all of them
     *
     * @param pack source pack
     */
    private int importPack(DocumentFile pack) {
        int stickersInPack = 0;
        DocumentFile[] stickers = pack.listFiles();
        for (int i = 0; i < stickers.length; i++) {
            stickersInPack += importSticker(stickers[i], pack.getName() + "/");
        }
        return stickersInPack;
    }

    /**
     * Copies stickers from source to internal storage
     *
     * @param sticker sticker to copy over
     * @param pack    the pack which the sticker belongs to
     */
    private int importSticker(DocumentFile sticker, String pack) {
        if (!canImportSticker(sticker)) {
            return 0;
        }
        //this path business is a little icky....
        File destSticker = new File(getFilesDir(), "stickers/" + pack + sticker.getName());
        destSticker.getParentFile().mkdirs();
        try {
            InputStream is = getContentResolver().openInputStream(sticker.getUri());
            Files.copy(is, destSticker.toPath());
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 1;

    }

    /**
     * Import files from storage to internal directory
     */
    public void importStickers() {
        //Use worker thread because this takes several seconds
        new FSCopyTask().execute(this);
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
                editor.putString("lastUpdateDate", Calendar.getInstance().getTime().toString());
                editor.apply();
                refreshStickerDirPath();
                importStickers();
            }
        }
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
        String lastUpdateDate = sharedPref.getString("lastUpdateDate", "never");
        int numStickersImported = sharedPref.getInt("numStickersImported", 0);

        TextView dirStatus = findViewById(R.id.stickerDirStatus);
        dirStatus.setText(stickerDirPath + " on " + lastUpdateDate + " with " +
                numStickersImported + " stickers loaded.");
    }

    /**
     * AsyncTask to handle file copying
     */
    private class FSCopyTask extends AsyncTask<Context, Void, Integer> {

        /**
         * @param params should include a single Context
         * @return the number of stickers successfully added
         */
        protected Integer doInBackground(Context... params) {
            Context context = params[0];
            File oldStickers = new File(getFilesDir(), "stickers");
            deleteRecursive(oldStickers);
            int stickersInDir = 0;
            //could pass path as parameter, but we save it for the textbox anyways
            //guaranteed to be non-null since onActivityResult only imports if result ok
            String stickerDirPath = sharedPref.getString("stickerDirPath", "none set");
            DocumentFile tree = DocumentFile.fromTreeUri(context, Uri.parse(stickerDirPath));

            DocumentFile[] files = tree.listFiles();

            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile())
                    stickersInDir += importSticker(files[i], "");
                if (files[i].isDirectory())
                    stickersInDir += importPack(files[i]);
            }
            return Integer.valueOf(stickersInDir);
        }

        /**
         * Has to be in AsyncTask's onPostExecute() to update UI.
         *
         * @param result the number of stickers imported in doInBackground()
         */
        protected void onPostExecute(Integer result) {
            Toast.makeText(getApplicationContext(),
                    "Imported " + result.toString() + " stickers. You may need to reload the keyboard for new stickers to show up.",
                    Toast.LENGTH_SHORT).show();

            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt("numStickersImported", result.intValue());
            editor.apply();
            refreshStickerDirPath();

            Button button = findViewById(R.id.chooseStickerDir);
            button.setEnabled(true);
        }

        protected void onPreExecute() {
            Toast.makeText(getApplicationContext(),
                    "Starting import. You will not be able to reselect directory until finished. This might take a bit!",
                    Toast.LENGTH_SHORT).show();
            Button button = findViewById(R.id.chooseStickerDir);
            button.setEnabled(false);
        }
    }
}
