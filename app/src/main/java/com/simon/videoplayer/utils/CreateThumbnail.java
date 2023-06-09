package com.simon.videoplayer.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Size;

import com.simon.videoplayer.model.FileModel;

import java.io.IOException;

public class CreateThumbnail extends CreateBitmap {

    private final FileModel fileModel;

    public CreateThumbnail(Context context, FileModel fileModel) {
        super(context);
        this.fileModel = fileModel;
    }

    public Bitmap getVideoThumbnailBitmap() throws IOException {
        Uri uri = Uri.parse(MediaStore.Video.Media.EXTERNAL_CONTENT_URI.toString() + "/" + fileModel.getId());
        return context.getContentResolver().loadThumbnail(uri, new Size(200, 200), null);
    }
}
