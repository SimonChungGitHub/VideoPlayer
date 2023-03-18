package com.simon.videoplayer;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.PointF;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.simon.videoplayer.model.FileModel;
import com.simon.videoplayer.sqlite.VideoSqliteOpenHelper;

import java.nio.file.Paths;
import java.util.Objects;

public class VideoPlayActivity extends AppCompatActivity {

    /**
     * 耳機插入
     */
    public final BroadcastReceiver headsetPlugReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                if (intent.hasExtra("state")) {
                    if (intent.getIntExtra("state", 0) == 0) {
                        //耳機拔除
                        videoView.pause();
                    } else if (intent.getIntExtra("state", 0) == 1) {
                        //耳機插入
                        videoView.start();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * 藍芽廣播
     **/
    public final BroadcastReceiver bluetoothReceive = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                int bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, 0);
                if (action.equals(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
                        && bluetoothState == BluetoothAdapter.STATE_DISCONNECTED) {
                    //藍芽斷開
                    audioManager.abandonAudioFocusRequest(focusRequest);
                    videoView.pause();
                    mediaController.show(0);
                } else if (action.equals(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
                        && bluetoothState == BluetoothAdapter.STATE_CONNECTED) {
                    //藍芽連線
                    audioManager.requestAudioFocus(focusRequest);
                    videoView.start();
                    mediaController.hide();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private FileModel model;
    private VideoView videoView;
    private MediaController mediaController;
    private TextView fileTitle;
    private AudioManager audioManager;
    private AudioFocusRequest focusRequest;
    private final PointF startPoint = new PointF();

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                startPoint.set((int) ev.getX(), (int) ev.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                int width = displayMetrics.widthPixels;
                float moveY = ev.getY() - startPoint.y;
                if (ev.getX() > width / 2f) {
                    if (moveY < -200) {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                    } else if (moveY > 200) {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                    }
                } else {
                    if (moveY < -200) {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    } else if (moveY > 200) {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                    }
                }
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            getWindow().addFlags(View.SYSTEM_UI_FLAG_FULLSCREEN);
            Objects.requireNonNull(getSupportActionBar()).hide();
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setContentView(R.layout.activity_video_play);
            videoView = findViewById(R.id.videoView2);
            fileTitle = findViewById(R.id.video_view_title);
            registerReceiver(headsetPlugReceiver, new IntentFilter("android.intent.action.HEADSET_PLUG"));
            registerReceiver(bluetoothReceive, new IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED));
            audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            AudioAttributes playbackAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(i -> {
                        switch (i) {
                            case AudioManager.AUDIOFOCUS_LOSS:
                            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                                videoView.pause();
                                break;
                            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                                break;
                            case AudioManager.AUDIOFOCUS_GAIN:
                                videoView.start();
                                break;
                        }
                    })
                    .build();
            audioManager.requestAudioFocus(focusRequest);

            model = getIntent().getParcelableExtra("model");
            model.setContext(this);
            videoView.setVideoPath(model.getPath());
            String name = Paths.get(model.getPath()).toFile().getName();
            fileTitle.setText(name.substring(0, name.indexOf(".")));
            mediaController = new MediaController(this) {
                @Override
                public void show(int timeout) {
                    if (timeout == 3000) timeout = 0;
                    super.show(timeout);
                    fileTitle.setVisibility(View.VISIBLE);
                }

                @Override
                public void hide() {
                    super.hide();
                    fileTitle.setVisibility(View.GONE);
                }

                @Override
                public boolean dispatchKeyEvent(KeyEvent event) {
                    if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                        super.hide();
                        finish();
                        return true;
                    }
                    return super.dispatchKeyEvent(event);
                }
            };
            mediaController.setAnchorView(videoView);
            videoView.setOnPreparedListener(mp -> {
                videoView.setMediaController(mediaController);
                videoView.requestFocus();
                videoView.seekTo(model.getPosition());
                videoView.start();
            });
            videoView.setOnCompletionListener(mp -> {
                backupCurrentPosition(model, videoView.getCurrentPosition());
                try (VideoSqliteOpenHelper obj = new VideoSqliteOpenHelper(this)) {
                    SQLiteDatabase db = obj.getWritableDatabase();
                    db.delete(VideoSqliteOpenHelper._TableName, "video_id=?", new String[]{String.valueOf(model.getId())});
                } catch (Exception e) {
                    Log.e("setOnCompletionListener", e.toString());
                }
                mp.seekTo(0);
                finish();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        model.setPosition(videoView.getCurrentPosition());
        backupCurrentPosition(model, videoView.getCurrentPosition());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bluetoothReceive);
        unregisterReceiver(headsetPlugReceiver);
        if (mediaController.isShowing()) {
            mediaController.hide();
        }
        if (videoView != null) {
            videoView.stopPlayback();
        }
        audioManager.abandonAudioFocusRequest(focusRequest);
    }

    public void backupCurrentPosition(FileModel model, int currentPosition) {
        try (VideoSqliteOpenHelper obj = new VideoSqliteOpenHelper(this)) {
            SQLiteDatabase db = obj.getWritableDatabase();
            String sql = "select * from " + VideoSqliteOpenHelper._TableName + " where video_id=?";
            try (Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(model.getId())})) {
                if (cursor.getCount() > 0) {
                    db.delete(VideoSqliteOpenHelper._TableName, "video_id=?", new String[]{String.valueOf(model.getId())});
                }
                ContentValues values = new ContentValues();
                values.put("video_id", model.getId());
                values.put("position", currentPosition);
                db.insert(VideoSqliteOpenHelper._TableName, null, values);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}