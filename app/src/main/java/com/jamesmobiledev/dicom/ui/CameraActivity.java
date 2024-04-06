package com.jamesmobiledev.dicom.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.TorchState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.jamesmobiledev.dicom.R;
import com.jamesmobiledev.dicom.databinding.ActivityCameraBinding;
import com.jamesmobiledev.dicom.model.DicomData;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import timber.log.Timber;

public class CameraActivity extends AppCompatActivity {
    private ActivityCameraBinding binding;
    private PreviewView viewFinder;
    private TextView photoCountText;
    private boolean isLongPress = false;
    private boolean isCapturing = false;
    private final long LONG_PRESS_DURATION = 1500; // 2 seconds for long press
    private final Handler longPressHandler = new Handler(Looper.getMainLooper());
    private final List<Bitmap> capturedImages = new ArrayList<>();
    private final List<Uri> capturedImageUris = new ArrayList<>();
    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private CameraSelector cameraSelector;
    private Preview preview;
    private ImageCapture imageCapture;
    Size targetResolution = new Size(1080, 1920);
    String dicomDataJson;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCameraBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            dicomDataJson = extras.getString("dicomData");
        }

        initViews();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initViews() {
        viewFinder = binding.camPreview;
        photoCountText = binding.photoCountText;

        startCamera();


        binding.cameraCaptureButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isLongPress = false;
                    longPressHandler.postDelayed(longPressRunnable, LONG_PRESS_DURATION);
                    return true;
                case MotionEvent.ACTION_UP:
                    longPressHandler.removeCallbacks(longPressRunnable);
                    if (!isLongPress) {
                        captureSinglePhoto();
                    } else {
                        stopContinuousCapture();
                    }
                    return true;
            }
            return false;
        });


        binding.btnCancel.setOnClickListener(view -> {
            this.finish();
        });

        binding.btnFlash.setOnClickListener(view -> {
            toggleTorch();
        });
    }

    private void captureSinglePhoto() {
        File photoFile;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            return;
        }

        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                Uri savedUri = outputFileResults.getSavedUri();
                if (savedUri == null) {
                    savedUri = Uri.fromFile(photoFile);
                }
                singleCapture(savedUri);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {

            }
        });
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName,  /* prefix */
                ".jpg",   /* suffix */
                storageDir      /* directory */);

        return image;
    }

    private void startContinuousCapture() {
        capturedImages.clear();
        isCapturing = true;
        binding.photoCountText.setVisibility(View.VISIBLE);
        continuousCapture(0);
    }

    private void stopContinuousCapture() {
        binding.photoCountText.setText("0");
        binding.photoCountText.setVisibility(View.GONE);
        isCapturing = false;
        multipleCapture(new ArrayList<>(capturedImageUris)); // Pass a copy of the captured images
    }

    private void continuousCapture(int count) {
        if (count >= 20 || !isCapturing) {
            stopContinuousCapture();
            return;
        }

        File photoFile;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {

            return;
        }

        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        // Capture logic
        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                Uri savedUri = outputFileResults.getSavedUri();
                if (savedUri == null) savedUri = Uri.fromFile(photoFile);

                capturedImageUris.add(savedUri);
                Timber.tag("@@@").d("ImageCapture = " + count);

                runOnUiThread(() -> photoCountText.setText(String.valueOf(count + 1)));

                // Continue capturing if still pressing
                if (isCapturing) {
                    continuousCapture(count + 1);
                }

            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {

            }

        });
    }


    private Bitmap imageProxyToBitmap(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
    }

    private void singleCapture(Uri uri) {
        ArrayList<String> uriStrings = new ArrayList<>();
        uriStrings.add(uri.toString());

        Intent intent = new Intent(this, CameraResultActivity.class);
        intent.putStringArrayListExtra("uris", uriStrings);
        intent.putExtra("dicomData", dicomDataJson);
        startActivity(intent);
    }

    private void multipleCapture(List<Uri> capturedImages) {
        ArrayList<String> uriStrings = new ArrayList<>();
        for (Uri uri : capturedImages) {
            uriStrings.add(uri.toString());
        }

        Intent intent = new Intent(this, CameraResultActivity.class);
        intent.putStringArrayListExtra("uris", uriStrings);
        intent.putExtra("dicomData", dicomDataJson);
        startActivity(intent);
    }

    private final Runnable longPressRunnable = new Runnable() {
        @Override
        public void run() {
            isLongPress = true;
            startContinuousCapture();
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                preview = new Preview.Builder().setTargetResolution(targetResolution).build();

                imageCapture = new ImageCapture.Builder().setTargetResolution(targetResolution)
                        .setTargetRotation(viewFinder.getDisplay().getRotation())
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build();

                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                cameraProvider.unbindAll();
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
            } catch (ExecutionException | InterruptedException e) {
                // Handle exceptions
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void toggleTorch() {
        if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
            boolean torchState = camera.getCameraInfo().getTorchState().getValue() == TorchState.ON;
            camera.getCameraControl().enableTorch(!torchState);
            if (torchState) {
                binding.btnFlash.setIconResource(R.drawable.flash_off);
            } else {
                binding.btnFlash.setIconResource(R.drawable.flash_on);

            }
        }
    }


}