package com.simon.videoplayer.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class VideoSqliteOpenHelper extends SQLiteOpenHelper {
    private final static int _DBVersion = 1;
    private final static String _DBName = "video.db";
    public final static String _TableName = "Video";

    public VideoSqliteOpenHelper(@Nullable Context context) {
        super(context, _DBName, null, _DBVersion);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String SQL = "CREATE TABLE IF NOT EXISTS " + _TableName + "( " +
                "video_id LONG PRIMARY KEY, position LONG);";
        db.execSQL(SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        final String SQL = "DROP TABLE " + _TableName;
        db.execSQL(SQL);
    }
}
