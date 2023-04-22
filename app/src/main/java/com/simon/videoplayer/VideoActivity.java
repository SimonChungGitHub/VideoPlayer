package com.simon.videoplayer;

import android.Manifest;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.simon.videoplayer.model.FileModel;
import com.simon.videoplayer.utils.CreateThumbnail;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class VideoActivity extends AppCompatActivity {

    private GridView gridView;
    private ArrayList<FileModel> fileModels = new ArrayList<>();
    private viewAdapter imageAdapter;
    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        gridView = findViewById(R.id.gridView);
        gridView.setNumColumns(3);
        fileModels = loadVideoList();
        imageAdapter = new viewAdapter(this, fileModels);
        gridView.setAdapter(imageAdapter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_VIDEO}, 0);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (searchView != null) {
            searchView.clearFocus();
        }
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            gridView.setNumColumns(3);
        } else {
            gridView.setNumColumns(6);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        fileModels = loadVideoList();
        imageAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        try {
            getMenuInflater().inflate(R.menu.menu, menu);
            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            searchView = (SearchView) menu.findItem(R.id.search).getActionView();
            // Assumes current activity is the searchable activity (不然會 crash)
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setIconified(false);
            searchView.clearFocus();
            searchView.setQueryHint("請輸入查詢的影片名稱");
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    if (query.equals("")) {
                        imageAdapter = new viewAdapter(getApplicationContext(), fileModels);
                    } else {
                        ArrayList<FileModel> list = (ArrayList<FileModel>) fileModels.stream().filter(o -> o.getPath().toLowerCase().contains(query)).collect(Collectors.toList());
                        imageAdapter = new viewAdapter(getApplicationContext(), list);
                    }
                    gridView.setAdapter(imageAdapter);
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    if (newText.equals("")) {
                        imageAdapter = new viewAdapter(getApplicationContext(), fileModels);
                    } else {
                        ArrayList<FileModel> list = (ArrayList<FileModel>) fileModels.stream().filter(o -> o.getPath().toLowerCase().contains(newText)).collect(Collectors.toList());
                        imageAdapter = new viewAdapter(getApplicationContext(), list);
                    }
                    gridView.setAdapter(imageAdapter);
                    return false;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 0:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
                        }
                        gridView = findViewById(R.id.gridView);
                        gridView.setNumColumns(3);
                        fileModels = loadVideoList();
                        imageAdapter = new viewAdapter(this, fileModels);
                        gridView.setAdapter(imageAdapter);
                    }
                } else {
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                    builder.setMessage("開啟存取權限")
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_VIDEO}, 0);
                                else
                                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                            })
                            .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss());
                    Dialog dialog = builder.create();
                    dialog.show();
                }
                break;
            case 1:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                    builder.setMessage("開啟藍芽權限")
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    ActivityCompat.requestPermissions(this,
                                            new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss());
                    Dialog dialog = builder.create();
                    dialog.show();
                }
                break;
            default:
        }
    }

    public ArrayList<FileModel> loadVideoList() {
        ArrayList<FileModel> list = new ArrayList<>();
        try {
            Uri collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
            }
            try (Cursor cursor = getContentResolver().query(collection,
                    new String[]{MediaStore.Video.Media._ID, MediaStore.Video.Media.DATA,
                            MediaStore.Video.Media.DATE_ADDED, MediaStore.Video.Media.DURATION},
                    null, null, MediaStore.Video.Media.DATE_ADDED + " ASC")) {
                while (cursor.moveToNext()) {
                    FileModel model = new FileModel(this);
                    model.setId(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)));
                    model.setPath(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)));
                    model.setDuration(Integer.parseInt(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION))));
                    list.add(model);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private static class viewAdapter extends BaseAdapter {
        private final Context context;
        private final ArrayList<FileModel> models;

        public viewAdapter(Context context, ArrayList<FileModel> models) {
            this.context = context;
            this.models = models;
        }

        @Override
        public int getCount() {
            return models.size();
        }

        @Override
        public Object getItem(int position) {
            return models.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            try {
                if (convertView == null) {
                    convertView = LayoutInflater.from(context).inflate(R.layout.grid_item, parent, false);
                }
                FileModel model = models.get(position);
                TextView title = convertView.findViewById(R.id.video_title);
                String name = Paths.get(model.getPath()).toFile().getName();
                title.setText(name.substring(0, name.indexOf(".")));
                ImageView imageView = convertView.findViewById(R.id.imageView);
                ProgressBar progressBar = convertView.findViewById(R.id.progressBar);
                progressBar.setMax(model.getDuration());
                int currentPosition = model.getPosition();
                if (currentPosition == 0) {
                    progressBar.setVisibility(View.GONE);
                } else if (currentPosition >= model.getDuration()) {
                    model.setPosition(0);
                    progressBar.setVisibility(View.GONE);
                } else {
                    progressBar.setProgress(model.getPosition());
                    progressBar.setVisibility(View.VISIBLE);
                }

                imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                CreateThumbnail createBitmap = new CreateThumbnail(context, model);
                Bitmap bitmap = createBitmap.roundedCornerBitmap(createBitmap.getVideoThumbnailBitmap(), 10);
                if (bitmap != null) {

                    imageView.setImageBitmap(bitmap);
                }
                imageView.setOnClickListener(view -> {
                    Intent intent = new Intent(context, VideoPlayActivity.class);
                    intent.putExtra("model", model);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                });
                imageView.setOnLongClickListener(v -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                            !Environment.isExternalStorageManager()) {
                        new AlertDialog.Builder(context)
                                .setMessage("無刪除檔案權限, 是否開啟權限")
                                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    context.startActivity(intent);
                                })
                                .show();
                        return false;
                    }
                    File file = new File(model.getPath());
                    new AlertDialog.Builder(context)
                            .setMessage("刪除檔案 " + file.getName())
                            .setNegativeButton(android.R.string.no, (dialog, which) -> dialog.dismiss())
                            .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                                model.deleteVideo(context, model.getId());
                                dialog.dismiss();
                                models.remove(model);
                                notifyDataSetChanged();
                            })
                            .show();
                    return false;
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
            return convertView;
        }
    }


}