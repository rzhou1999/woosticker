package com.woosticker;

import android.content.ClipDescription;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.InputMethodService;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.inputmethod.EditorInfo;
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

import com.github.penfeizhou.animation.apng.APNGDrawable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class ImageKeyboard extends InputMethodService {

    // Constants
    private static final String AUTHORITY = "com.woosticker.inputcontent";

    // Attributes
    private Map<String, String> SUPPORTED_MIMES;
    private HashMap<String, StickerPack> loadedPacks = new HashMap<>();
    private LinearLayout ImageContainer;
    private LinearLayout PackContainer;
    private File INTERNAL_DIR;
    private int iconsPerRow;
    private int iconSize;
    private SharedPreferences sharedPref;

    /**
     * Adds a back button as a PackCard to keyboard that shows the InputMethodPicker
     */
    private void addBackButtonToContainer() {
        CardView PackCard = (CardView) getLayoutInflater().inflate(R.layout.pack_card, PackContainer, false);
        ImageButton BackButton = PackCard.findViewById(R.id.ib3);
        Drawable icon = ResourcesCompat.getDrawable(getResources(), R.drawable.tabler_icon_arrow_back_white, null);
        BackButton.setImageDrawable(icon);
        BackButton.setOnClickListener(view -> {
            InputMethodManager inputMethodManager = (InputMethodManager) getApplicationContext()
                    .getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.showInputMethodPicker();
        });
        PackContainer.addView(PackCard);
    }

    /**
     * Adds a pack card to the keyboard from a StickerPack
     *
     * @param pack: StickerPack - the sticker pack to add
     */
    private void addPackToContainer(StickerPack pack) {
        CardView PackCard = (CardView) getLayoutInflater().inflate(R.layout.pack_card, PackContainer, false);
        ImageButton PackButton = PackCard.findViewById(R.id.ib3);
        setPackButtonImage(pack, PackButton);
        PackButton.setTag(pack);
        PackButton.setOnClickListener(view -> {
            ImageContainer.removeAllViewsInLayout();
            recreateImageContainer((StickerPack) view.getTag());
        });
        PackContainer.addView(PackCard);
    }


    /**
     * In the event that a mimetype is unsupported by a InputConnectionCompat (looking at you, Signal)
     * Create a temporary png and send that. In the event that png is not supported, create a toast as before
     *
     * @param file: File
     */
    private void doFallbackCommitContent(@NonNull File file) {
        // PNG might not be supported so fallback to toast
        if (SUPPORTED_MIMES.get(".png") == null) {
            Toast.makeText(getApplicationContext(), Utils.getFileExtension(file.getName()) +
                    " not supported here.", Toast.LENGTH_LONG).show();
            return;
        }
        // Create a new compatSticker and convert the sticker to png
        File compatSticker = new File(getFilesDir(), "stickers/__compatSticker__/__compatSticker__.png");
        if (compatSticker.getParentFile() != null) {
            compatSticker.getParentFile().mkdirs(); // Protect against null pointer exception
        }
        try {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)).compress(Bitmap.CompressFormat.PNG, 90, new FileOutputStream(compatSticker));
        } catch (IOException ignore) {
        }
        // Send the compatSticker!
        doCommitContent("description", "image/png", compatSticker);
    }

    /**
     * @param description: String
     * @param mimeType:    String
     * @param file:        File
     */
    private void doCommitContent(@NonNull String description, @NonNull String mimeType,
                                 @NonNull File file) {
        final Uri contentUri = FileProvider.getUriForFile(this, AUTHORITY, file);
        final int flag = InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION;
        final InputContentInfoCompat inputContentInfoCompat = new InputContentInfoCompat(
                contentUri,
                new ClipDescription(description, new String[]{mimeType}),
                null);
        InputConnectionCompat.commitContent(
                getCurrentInputConnection(), getCurrentInputEditorInfo(), inputContentInfoCompat,
                flag, null);
    }

    /**
     * Apply a sticker file to the image button
     *
     * @param sticker: File - the file object representing the sticker
     * @param btn:     ImageButton - the button
     */
    private void setStickerButtonImage(File sticker, ImageButton btn) {
        String sName = sticker.getName();
        // Create drawable from file
        Drawable drawable = null;
        try {
            drawable = ImageDecoder.decodeDrawable(ImageDecoder.createSource(sticker));
        } catch (IOException ignore) {
        }
        if (sName.contains(".png") || sName.contains(".apng")) {
            drawable = APNGDrawable.fromFile(sticker.getAbsolutePath());
            ((APNGDrawable) drawable).setAutoPlay(false);
            ((APNGDrawable) drawable).start();
        }
        // Disable animations?
        if (drawable instanceof AnimatedImageDrawable && !sharedPref.getBoolean("disable_animations", false)) {
            ((AnimatedImageDrawable) drawable).start();
        }
        if (drawable instanceof APNGDrawable && sharedPref.getBoolean("disable_animations", false)) {
            ((APNGDrawable) drawable).stop();
        }
        // Apply
        btn.setImageDrawable(drawable);
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
     * Check if the sticker is supported by the receiver
     *
     * @param editorInfo: EditorInfo - the editor/ receiver
     * @param mimeType:   String - the image mimetype
     * @return boolean - is the mimetype supported?
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
        sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        iconsPerRow = sharedPref.getInt("iconsPerRow", 3);
        iconSize = sharedPref.getInt("iconSize", 160);
        reloadPacks();
    }

    @NonNull
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
    public void onStartInputView(@Nullable EditorInfo info, boolean restarting) {
        SUPPORTED_MIMES = Utils.get_supported_mimes();
        boolean allSupported = true;
        String[] mimesToCheck = SUPPORTED_MIMES.keySet().toArray(new String[0]);
        for (String s : mimesToCheck) {
            boolean mimeSupported = isCommitContentSupported(info, SUPPORTED_MIMES.get(s));

            allSupported = allSupported && mimeSupported;
            if (!mimeSupported) {
                SUPPORTED_MIMES.remove(s);
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
            if ((i % iconsPerRow) == 0) {
                ImageContainerColumn = (LinearLayout) getLayoutInflater().inflate(R.layout.image_container_column, ImageContainer, false);
            }

            CardView ImageCard = (CardView) getLayoutInflater().inflate(R.layout.sticker_card, ImageContainerColumn, false);
            ImageButton ImgButton = ImageCard.findViewById(R.id.ib3);
            ImgButton.getLayoutParams().height = iconSize;
            ImgButton.getLayoutParams().width = iconSize;
            setStickerButtonImage(stickers[i], ImgButton);
            ImgButton.setTag(stickers[i]);
            ImgButton.setOnClickListener(view -> {
                final File file = (File) view.getTag();

                String stickerType = SUPPORTED_MIMES.get(Utils.getFileExtension(file.getName()));

                if (stickerType == null) {
                    doFallbackCommitContent(file);
                    return;
                }

                doCommitContent(file.getName(), stickerType, file);
            });

            ImageContainerColumn.addView(ImageCard);

            if ((i % iconsPerRow) == 0) {
                ImageContainer.addView(ImageContainerColumn);
            }
        }
    }

    private void recreatePackContainer() {
        PackContainer.removeAllViewsInLayout();
        // Back button
        if (sharedPref.getBoolean("showBackButton", false)) {
            addBackButtonToContainer();
        }
        // Packs
        String[] sortedPackNames = loadedPacks.keySet().toArray(new String[0]);
        Arrays.sort(sortedPackNames);
        for (String sortedPackName : sortedPackNames) {
            addPackToContainer(loadedPacks.get(sortedPackName));
        }
        if (sortedPackNames.length > 0) {
            recreateImageContainer(Objects.requireNonNull(loadedPacks.get(sortedPackNames[0])));
        }
    }

    public void reloadPacks() {
        loadedPacks = new HashMap<>();
        INTERNAL_DIR = new File(getFilesDir(), "stickers");

        File[] packs = INTERNAL_DIR.listFiles(File::isDirectory);

        if (packs != null) {
            for (File file : packs) {
                StickerPack pack = new StickerPack(file);
                if (pack.getStickerList().length > 0) {
                    loadedPacks.put(file.getName(), pack);
                }
            }
        }

        File[] baseStickers = INTERNAL_DIR.listFiles(File::isFile);

        if (baseStickers != null && baseStickers.length > 0) {
            loadedPacks.put("", new StickerPack(INTERNAL_DIR));
        }
    }

    private boolean validatePackageName(@Nullable EditorInfo editorInfo) {
        if (editorInfo == null) {
            return false;
        }
        final String packageName = editorInfo.packageName;
        return packageName != null;
    }

}