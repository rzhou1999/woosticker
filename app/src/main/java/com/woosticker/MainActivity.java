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
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class MainActivity extends Activity {
    private final int CHOOSE_STICKER_DIR = 62519;
    private SharedPreferences sharedPref = null;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        setContentView(R.layout.activity_main);
        refreshStickerDirPath();
    }

    private void refreshStickerDirPath(){
        String stickerDirPath = sharedPref.getString("stickerDirPath", "none set");

        TextView dirStatus = findViewById(R.id.stickerDirStatus);
        dirStatus.setText(stickerDirPath);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHOOSE_STICKER_DIR && resultCode == Activity.RESULT_OK) {
            if (data != null){
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("stickerDirPath", ((Uri) data.getData()).toString());
                editor.apply();
                refreshStickerDirPath();
            }
        }
    }

    public void copyBufferedFile(BufferedInputStream bufferedInputStream,
                                 BufferedOutputStream bufferedOutputStream)
            throws IOException
    {
        try (BufferedInputStream in = bufferedInputStream;
             BufferedOutputStream out = bufferedOutputStream)
        {
            byte[] buf = new byte[1024];
            int nosRead;
            while ((nosRead = in.read(buf)) != -1)  // read this carefully ...
            {
                out.write(buf, 0, nosRead);
            }
        }
    }

    public void chooseDir(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, CHOOSE_STICKER_DIR);
    }

    public void importPack(DocumentFile pack){
        DocumentFile[] stickers = pack.listFiles();
        for (int i = 0; i < stickers.length; i++){
            importSticker(stickers[i], pack.getName() + "/");
        }
    }

    public void importSticker(DocumentFile sticker, String pack){
        File destSticker = new File(getFilesDir(), "stickers" + "/" + pack + sticker.getName());
        destSticker.getParentFile().mkdirs();
        try {
            InputStream is = getContentResolver().openInputStream(sticker.getUri());
            Files.copy(is, destSticker.toPath());
            is.close();
        } catch (IOException e) {
            //e.printStackTrace();
        }

    }

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

    public void deleteRecursive(File fileOrDirectory) {


        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }
        fileOrDirectory.delete();
    }

    //https://stackoverflow.com/a/46848226
    public static void triggerRebirth(Context context) {
        PackageManager packageManager = context.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(context.getPackageName());
        ComponentName componentName = intent.getComponent();
        Intent mainIntent = Intent.makeRestartActivityTask(componentName);
        context.startActivity(mainIntent);
        Runtime.getRuntime().exit(0);
    }
}
