package com.woosticker;

import android.app.AppOpsManager;
import android.content.ClipDescription;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.InputMethodService;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputBinding;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.inputmethod.EditorInfoCompat;
import androidx.core.view.inputmethod.InputConnectionCompat;
import androidx.core.view.inputmethod.InputContentInfoCompat;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import pl.droidsonroids.gif.GifDrawable;


public class ImageKeyboard extends InputMethodService {

    private static final String TAG = "ImageKeyboard";
    private static final String AUTHORITY = "com.woosticker.inputcontent";

    private Map<String, String> SUPPORTED_MIMES;
    private HashMap<String, StickerPack> loadedPacks = new HashMap<String, StickerPack>();
    private LinearLayout ImageContainer;
    private LinearLayout PackContainer;
    private File INTERNAL_DIR;
    private int iconsPerRow;
    private int iconSize;
    private SharedPreferences sharedPref;

    private void addBackButtonToContainer() {
        CardView PackCard = (CardView) getLayoutInflater().inflate(R.layout.pack_card, PackContainer, false);
        ImageButton BackButton = PackCard.findViewById(R.id.ib3);

        Resources res = this.getResources();
        Drawable icon = ResourcesCompat.getDrawable(res, R.drawable.tabler_icon_arrow_back_white, null);

        BackButton.setImageDrawable(icon);
        BackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InputMethodManager inputMethodManager = (InputMethodManager) getApplicationContext()
                        .getSystemService(INPUT_METHOD_SERVICE);
                inputMethodManager.showInputMethodPicker();
            }
        });
        PackContainer.addView(PackCard);
    }

    private void addPackToContainer(StickerPack pack) {
        CardView PackCard = (CardView) getLayoutInflater().inflate(R.layout.pack_card, PackContainer, false);
        ImageButton PackButton = PackCard.findViewById(R.id.ib3);
        setPackButtonImage(pack, PackButton);
        PackButton.setTag(pack);
        PackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ImageContainer.removeAllViewsInLayout();
                recreateImageContainer((StickerPack) view.getTag());
            }
        });
        PackContainer.addView(PackCard);
    }

    private void doCommitContent(@NonNull String description, @NonNull String mimeType,
                                 @NonNull File file) {
        final EditorInfo editorInfo = getCurrentInputEditorInfo();
        final Uri contentUri = FileProvider.getUriForFile(this, AUTHORITY, file);
        // On API 24 and prior devices, we cannot rely on
        // InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION. You as an IME author
        // need to decide what access control is needed (or not needed) for content URIs that
        // you are going to expose. This sample uses Context.grantUriPermission(), but you can
        // implement your own mechanism that satisfies your own requirements.
        try {
            grantUriPermission(
                    editorInfo.packageName, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception e) {
            Log.e(TAG, "grantUriPermission failed packageName=" + editorInfo.packageName
                    + " contentUri=" + contentUri, e);
        }

        final InputContentInfoCompat inputContentInfoCompat = new InputContentInfoCompat(
                contentUri,
                new ClipDescription(description, new String[]{mimeType}),
                null);
        InputConnectionCompat.commitContent(
                getCurrentInputConnection(), getCurrentInputEditorInfo(), inputContentInfoCompat,
                0, null);
    }

    /**
     * Apply a sticker file to the image button
     *
     * @param sticker: File - the file object representing the sticker
     * @param btn:     ImageButton - the button
     */
    private void setStickerButtonImage(File sticker, ImageButton btn) {
        String sName = sticker.getName();
        if (sticker.getName().contains(".gif")) {
            try {
                btn.setImageDrawable(new GifDrawable(sticker));
            } catch (IOException ignore) {
            }
        } else if (sName.contains(".webp")) {
            Glide.with(this).load(sticker.getAbsolutePath()).into(btn);
        } else if (sName.contains(".apng") || sName.contains(".png")) {
            Glide.with(this).load(sticker.getAbsolutePath()).into(btn);
        } else {
            btn.setImageDrawable(Drawable.createFromPath(sticker.getAbsolutePath()));
        }
        Drawable drawable = btn.getDrawable();
        if (drawable instanceof Animatable) {
            ((Animatable) drawable).stop();
        }

    }

    /**
     * Apply a sticker the the pack icon (imagebutton)
     *
     * @param pack: StickerPack - the stickerpack to grab the pack icon from
     * @param btn:  ImageButton - the button
     */
    private void setPackButtonImage(StickerPack pack, ImageButton btn) {
        setStickerButtonImage(pack.getThumbSticker(), btn);

    }

    /**
     * Check if the sticker is supported by the reciever
     *
     * @param editorInfo: EditorInfo - the editor/ receiver
     * @param mimeType:   String - the image mimetype
     * @return: boolean - is the mimetype supported?
     */
    private boolean isCommitContentSupported(
            @Nullable EditorInfo editorInfo, @NonNull String mimeType) {

        if (editorInfo == null) {
            return false;
        }

        final InputConnection ic = getCurrentInputConnection();
        if (ic == null) {
            return false;
        }

        if (!validatePackageName(editorInfo)) {
            return false;
        }

        final String[] supportedMimeTypes = EditorInfoCompat.getContentMimeTypes(editorInfo);

        for (String supportedMimeType : supportedMimeTypes) {

            if (ClipDescription.compareMimeTypes(mimeType, supportedMimeType)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        this.sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        this.iconsPerRow = this.sharedPref.getInt("iconsPerRow", 3);
        this.iconSize = sharedPref.getInt("iconSize", 160);

        reloadPacks();
    }

    @Override
    public View onCreateInputView() {


        RelativeLayout KeyboardLayout = (RelativeLayout) getLayoutInflater().inflate(R.layout.keyboard_layout, null);
        PackContainer = KeyboardLayout.findViewById(R.id.packContainer);
        ImageContainer = KeyboardLayout.findViewById(R.id.imageContainer);
        recreatePackContainer();


        return KeyboardLayout;
    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        // In full-screen mode the inserted content is likely to be hidden by the IME. Hence in this
        // sample we simply disable full-screen mode.
        return false;
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        SUPPORTED_MIMES = Utils.get_supported_mimes();
        boolean allSupported = true;
        String[] mimesToCheck = SUPPORTED_MIMES.keySet().toArray(new String[SUPPORTED_MIMES.size()]);
        for (int i = 0; i < mimesToCheck.length; i++) {
            boolean mimeSupported = isCommitContentSupported(info, SUPPORTED_MIMES.get(mimesToCheck[i]));

            allSupported = allSupported && mimeSupported;
            if (!mimeSupported) {
                SUPPORTED_MIMES.remove(mimesToCheck[i]);
            }

        }

        if (!allSupported) {
            Toast.makeText(getApplicationContext(),
                    "One or more image formats not supported here. Some stickers may not send correctly.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void recreateImageContainer(StickerPack pack) {

        final File imagesDir = new File(getFilesDir(), "stickers/" + pack);
        imagesDir.mkdirs();
        ImageContainer.removeAllViewsInLayout();
        LinearLayout ImageContainerColumn = (LinearLayout) getLayoutInflater().inflate(R.layout.image_container_column, ImageContainer, false);

        File[] stickers = pack.getStickerList();
        for (int i = 0; i < stickers.length; i++) {
            if ((i % this.iconsPerRow) == 0) {
                ImageContainerColumn = (LinearLayout) getLayoutInflater().inflate(R.layout.image_container_column, ImageContainer, false);
            }

            CardView ImageCard = (CardView) getLayoutInflater().inflate(R.layout.sticker_card, ImageContainerColumn, false);
            ImageButton ImgButton = ImageCard.findViewById(R.id.ib3);
            ImgButton.getLayoutParams().height = this.iconSize;
            ImgButton.getLayoutParams().width = this.iconSize;
            setStickerButtonImage(stickers[i], ImgButton);
            ImgButton.setTag(stickers[i]);
            ImgButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final File file = (File) view.getTag();

                    String stickerType = SUPPORTED_MIMES.get(Utils.getFileExtension(file.getName()));

                    if (stickerType == null) {
                        Toast.makeText(getApplicationContext(),
                                Utils.getFileExtension(file.getName()) + " not supported here.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    ImageKeyboard.this.doCommitContent(file.getName(), stickerType, file);
                }
            });

            ImageContainerColumn.addView(ImageCard);

            if ((i % this.iconsPerRow) == 0) {
                ImageContainer.addView(ImageContainerColumn);
            }
        }
    }

    private void recreatePackContainer() {
        PackContainer.removeAllViewsInLayout();

        if (this.sharedPref.getBoolean("showBackButton", false)) {
            addBackButtonToContainer();
        }

        String[] sortedPackNames = loadedPacks.keySet().toArray(new String[loadedPacks.size()]);
        Arrays.sort(sortedPackNames);
        for (int i = 0; i < sortedPackNames.length; i++) {
            addPackToContainer(loadedPacks.get(sortedPackNames[i]));
        }

        if (sortedPackNames.length > 0) {
            recreateImageContainer(loadedPacks.get(sortedPackNames[0]));
        }
    }

    public void reloadPacks() {
        loadedPacks = new HashMap<String, StickerPack>();
        INTERNAL_DIR = new File(getFilesDir(), "stickers");

        File[] packs = INTERNAL_DIR.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });

        if (packs != null) {
            for (int i = 0; i < packs.length; i++) {
                StickerPack pack = new StickerPack(packs[i]);
                if (pack.getStickerList().length > 0) {
                    loadedPacks.put(packs[i].getName(), pack);
                }
            }
        }

        File[] baseStickers = INTERNAL_DIR.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return !file.isDirectory();
            }
        });

        if (baseStickers != null && baseStickers.length > 0) {
            loadedPacks.put("", new StickerPack(INTERNAL_DIR));
        }
    }

    private boolean validatePackageName(@Nullable EditorInfo editorInfo) {
        if (editorInfo == null) {
            return false;
        }
        final String packageName = editorInfo.packageName;
        if (packageName == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return true;
        }

        final InputBinding inputBinding = getCurrentInputBinding();
        if (inputBinding == null) {
            // Due to b.android.com/225029, it is possible that getCurrentInputBinding() returns
            // null even after onStartInputView() is called.
            // TODO: Come up with a way to work around this bug....
            Log.e(TAG, "inputBinding should not be null here. "
                    + "You are likely to be hitting b.android.com/225029");
            return false;
        }
        final int packageUid = inputBinding.getUid();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            final AppOpsManager appOpsManager =
                    (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
            try {
                appOpsManager.checkPackage(packageUid, packageName);
            } catch (Exception e) {
                return false;
            }
            return true;
        }

        final PackageManager packageManager = getPackageManager();
        final String[] possiblePackageNames = packageManager.getPackagesForUid(packageUid);
        for (final String possiblePackageName : possiblePackageNames) {
            if (packageName.equals(possiblePackageName)) {
                return true;
            }
        }
        return false;
    }

}