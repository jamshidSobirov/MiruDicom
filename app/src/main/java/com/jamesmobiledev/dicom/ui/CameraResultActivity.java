package com.jamesmobiledev.dicom.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.google.gson.Gson;
import com.jamesmobiledev.dicom.DicomGenerator;
import com.jamesmobiledev.dicom.R;
import com.jamesmobiledev.dicom.adapter.ImageAdapter;
import com.jamesmobiledev.dicom.databinding.ActivityCameraResultBinding;
import com.jamesmobiledev.dicom.model.DicomData;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

import timber.log.Timber;

public class CameraResultActivity extends AppCompatActivity {
    private ActivityCameraResultBinding binding;
    DicomData dicomData;
    ArrayList<Uri> imageUris;

    private Dialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCameraResultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initViews();

    }

    private void initViews() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String dicomDataJson = extras.getString("dicomData");
            dicomData = new Gson().fromJson(dicomDataJson, DicomData.class);

            Timber.tag("@@@").d(dicomDataJson);
        }


        ArrayList<String> uriStrings = getIntent().getStringArrayListExtra("uris");
        if (uriStrings != null) {
            imageUris = new ArrayList<>();
            for (String uriString : uriStrings) {
                imageUris.add(Uri.parse(uriString));
            }
            setupAdapter(imageUris);

        } else {
            Toast.makeText(this, "Something went wrong, try again!", Toast.LENGTH_SHORT).show();
            this.finish();
        }

        binding.btnCancel.setOnClickListener(view -> {
            this.finish();
        });

        binding.btnCreate.setOnClickListener(view -> {
            showLoadingDialog();
            new DicomGenerator(this).convertImagesToDicom(dicomData, imageUris);
            dismissLoadingDialog();

            Intent intent = new Intent(this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });
    }

    private void setupAdapter(ArrayList<Uri> uris) {
        binding.recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        ImageAdapter adapter = new ImageAdapter(this, uris);
        binding.recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        deleteFilesInDirectory();
        super.onDestroy();
    }

    private void deleteFilesInDirectory() {
        File directory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (directory != null && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!file.delete()) {
                        Timber.tag("@@@").e("Failed to delete %s", file);
                    }
                }
            }
        }
    }

    private void showLoadingDialog() {
        if (loadingDialog == null) {
            loadingDialog = new Dialog(this);
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null);
            loadingDialog.setContentView(view);
            loadingDialog.setCancelable(false); // To prevent dismiss by back press
            loadingDialog.setCanceledOnTouchOutside(false); // To prevent dismiss by touching outside
        }
        loadingDialog.show();
    }

    private void dismissLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }
}