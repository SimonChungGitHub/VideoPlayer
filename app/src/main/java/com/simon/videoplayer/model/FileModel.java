package com.simon.videoplayer.model;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;

import com.simon.videoplayer.sqlite.VideoSqliteOpenHelper;

/**
 * Model for Activity 畫面顯示的影片 file
 */
public class FileModel implements Parcelable {
    private Context context;
    private long id;
    private String path;
    private int duration;
    private int position = 0;
    private boolean selected = false;

    public FileModel(Context context) {
        this.context = context;
    }

    protected FileModel(Parcel in) {
        id = in.readLong();
        path = in.readString();
        duration = in.readInt();
        position = in.readInt();
        selected = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(path);
        dest.writeInt(duration);
        dest.writeInt(position);
        dest.writeByte((byte) (selected ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<FileModel> CREATOR = new Creator<FileModel>() {
        @Override
        public FileModel createFromParcel(Parcel in) {
            return new FileModel(in);
        }

        @Override
        public FileModel[] newArray(int size) {
            return new FileModel[size];
        }
    };

    public void setContext(Context context) {
        this.context = context;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }


    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getPosition() {
        try (VideoSqliteOpenHelper obj = new VideoSqliteOpenHelper(context)) {
            SQLiteDatabase db = obj.getWritableDatabase();
            String sql = "select * from " + VideoSqliteOpenHelper._TableName + " where video_id=?";
            try (Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(id)})) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    position = cursor.getInt(1);
                } else {
                    position = 0;
                }
            } catch (Exception e) {
                Log.e("aaa", e.toString());
            }
        }
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    /**
     * 刪除 android media store 資料庫 video row
     */
    public void deleteVideo(Context context, long id) {
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            uri = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        }
        String where = MediaStore.Video.Media._ID + "=?";
        context.getContentResolver().delete(uri, where, new String[]{String.valueOf(id)});
    }

    public Uri getUrl() {
        return Uri.parse(MediaStore.Video.Media.EXTERNAL_CONTENT_URI.toString() + "/" + id);
    }

}
