package com.jamesmobiledev.dicom.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.jamesmobiledev.dicom.R;
import com.jamesmobiledev.dicom.adapter.DicomFileAdapter;
import com.jamesmobiledev.dicom.databinding.ActivityHomeBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class HomeActivity extends AppCompatActivity {
    private ActivityHomeBinding binding;
    private static final int STORAGE_PERMISSION_CODE = 101;
    private static final int REQUEST_CODE_PICK_FILE = 102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initViews();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (hasStoragePermissions()) {
            binding.permissionHandlerBox.setVisibility(View.GONE);
            binding.recyclerView.setVisibility(View.VISIBLE);
            loadDicomFiles();
        } else {
            binding.permissionHandlerBox.setVisibility(View.VISIBLE);
            binding.recyclerView.setVisibility(View.GONE);
        }
    }

    private void initViews() {

        binding.btnGrantPermissions.setOnClickListener(view -> {
            requestStoragePermissions();
        });

    }

    private void loadDicomFiles() {
        File directory = new File(getExternalFilesDir(null), "MiruDicom");
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                Toast.makeText(this, "couldn't create the folder!", Toast.LENGTH_SHORT).show();
                return;
            }
        }


        File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".dcm"));

        ArrayList<File> filesList = new ArrayList<>();

        if (files == null || files.length == 0) {
            binding.tvDicomFilesStatus.setVisibility(View.VISIBLE);
            return;
        } else {
            Collections.addAll(filesList, files);
            binding.tvDicomFilesStatus.setVisibility(View.GONE);
        }

        DicomFileAdapter adapter = new DicomFileAdapter(this, filesList);
        binding.recyclerView.setAdapter(adapter);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter.setOnItemRemovedListener(new DicomFileAdapter.OnItemRemovedListener() {
            @Override
            public void onItemRemoved() {
                loadDicomFiles();//called when any item removed
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.createDcmFile:
                if (hasStoragePermissions()) {
                    Intent intent = new Intent(this, DicomGenerateActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Please, grant storage permissions!", Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.loadDcmFile:
                openFileChooser();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean hasStoragePermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissions granted!", Toast.LENGTH_SHORT).show();
                binding.permissionHandlerBox.setVisibility(View.GONE);
                loadDicomFiles();
            } else {
                binding.recyclerView.setVisibility(View.GONE);
                binding.permissionHandlerBox.setVisibility(View.VISIBLE);
                requestPermissionWithRationaleCheck();
            }
        }
    }

    private void requestPermissionWithRationaleCheck() {
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) || !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            showSettingsDialog();
        } else {
            Toast.makeText(this, "Permissions not granted!", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permission Required");
        builder.setMessage("The Storage permission is necessary for the app to function. Please enable it in app settings.");
        builder.setPositiveButton("Go to Settings", (dialog, which) -> {
            dialog.cancel();
            openAppSettings();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, 101);
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/dicom"); // Use "application/dicom" if DICOM files have a standard MIME type
        String[] mimetypes = {"application/dicom"}; // Adjust MIME types as needed
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);

        startActivityForResult(intent, REQUEST_CODE_PICK_FILE); // REQUEST_CODE_PICK_FILE is an integer constant
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PICK_FILE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Intent intent = new Intent(this, DicomDetailsActivity.class);
                intent.putExtra("dicomFileUri", data.getData().toString());
                startActivity(intent);
            }
        }
    }
}