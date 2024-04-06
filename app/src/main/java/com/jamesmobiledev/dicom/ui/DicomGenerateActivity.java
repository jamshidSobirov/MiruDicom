package com.jamesmobiledev.dicom.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import com.google.gson.Gson;
import com.jamesmobiledev.dicom.R;
import com.jamesmobiledev.dicom.databinding.ActivityDicomGenerateBinding;
import com.jamesmobiledev.dicom.model.DicomData;

public class DicomGenerateActivity extends AppCompatActivity {
    ActivityDicomGenerateBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDicomGenerateBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initViews();
    }

    private void initViews() {
        binding.btnGenerateDcm.setOnClickListener(view -> {
            if (hasCameraPermissions()) {
                DicomData dicomData = collectDicomData();
                openCameraActivity(dicomData);
            } else {
                requestCameraPermissions();
            }
        });

        binding.btnBack.setOnClickListener(view -> {
            this.finish();
        });
    }

    private void openCameraActivity(DicomData dicomData) {
        Intent intent = new Intent(DicomGenerateActivity.this, CameraActivity.class);
        intent.putExtra("dicomData", new Gson().toJson(dicomData));
        startActivity(intent);
    }

    private DicomData collectDicomData() {
        DicomData data = new DicomData();

        data.setPatientId(String.valueOf(System.currentTimeMillis()));
        data.setDoctorName(binding.tvDoctorName.getText().toString());
        data.setDoctorAge(Integer.parseInt(binding.tvDoctorAge.getText().toString()));
        data.setDoctorSex(binding.rgDoctorSex.getCheckedRadioButtonId() == R.id.rbDoctorMale ? "Male" : "Female");
        data.setPatientName(binding.tvPatientName.getText().toString());
        data.setPatientAge(binding.tvPatientAge.getText().toString());
        data.setPatientSex(binding.rgPatientSex.getCheckedRadioButtonId() == R.id.rbPatientMale ? "Male" : "Female");
        data.setImageDate(binding.tvImageDate.getText().toString());
        data.setImageTime(binding.tvImageTime.getText().toString());
        data.setImageColorInfo(binding.rgImageColorSpace.getCheckedRadioButtonId() == R.id.rbRGB ? "RGB" : "Grayscale");
        data.setImageComments(binding.tvImageDesc.getText().toString());

        return data;
    }

    private boolean hasCameraPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1001);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissions granted!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, CameraActivity.class));
            } else {
                requestPermissionWithRationaleCheck();
            }
        }
    }

    private void requestPermissionWithRationaleCheck() {
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
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
}