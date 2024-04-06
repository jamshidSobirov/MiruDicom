package com.jamesmobiledev.dicom.ui;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.imebra.CodecFactory;
import com.imebra.DataSet;
import com.imebra.DrawBitmap;
import com.imebra.Image;
import com.imebra.Memory;
import com.imebra.StreamReader;
import com.imebra.TagId;
import com.imebra.TransformsChain;
import com.imebra.drawBitmapType_t;
import com.jamesmobiledev.dicom.R;
import com.jamesmobiledev.dicom.databinding.ActivityDicomDetailsBinding;
import com.jamesmobiledev.dicom.model.DicomData;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.util.SafeClose;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DicomDetailsActivity extends AppCompatActivity {

    ActivityDicomDetailsBinding binding;
    ArrayList<byte[]> frames;

    DicomData dicomData;

    private Dialog loadingDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDicomDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initViews();
    }

    private void initViews() {
        Bundle extras = getIntent().getExtras();
        String dicomFilePath;
        String dicomFileUri;

        if (extras != null) {
            dicomFilePath = extras.getString("dicomFilePath");
            dicomFileUri = extras.getString("dicomFileUri");

            try {
                if (dicomFilePath != null && !dicomFilePath.isEmpty()) {
                    setupDicomFile(dicomFilePath, false);
                } else {
                    setupDicomFile(dicomFileUri, true);
                }
            } catch (Exception e) {
                Toast.makeText(DicomDetailsActivity.this, "Something wrong with dicom file", Toast.LENGTH_SHORT).show();
                this.finish();
            }

        } else {
            dicomFilePath = "";
            Toast.makeText(DicomDetailsActivity.this, "Something wrong with dicom file", Toast.LENGTH_SHORT).show();
            this.finish();
        }

        binding.btnBack.setOnClickListener(view -> {
            this.finish();
        });

        binding.imageContainer.setOnClickListener(view -> {
            openDicomImageViewer();
        });

        binding.btnClose.setOnClickListener(view -> {
            closeDicomImageViewer();
        });

        binding.btnShare.setOnClickListener(view -> {
            shareDicomFile(dicomFilePath);
        });

    }

    private void openDicomImageViewer() {
        try {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.black));

            binding.dicomImageContainer.setVisibility(View.VISIBLE);
            binding.ivDicom.setImageBitmap(convertByteArrayToBitmap(frames.get(0), dicomData.getColumns(), dicomData.getRows(), dicomData.getSamplesPerPixel()));


            binding.seekBar.setMax(frames.size() - 1);
            binding.numberOfFrames.setText("Number of frames: " + frames.size());

            binding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    binding.frameNumber.setText("Frame - " + (progress + 1));
                    binding.ivDicom.setImageBitmap(convertByteArrayToBitmap(frames.get(progress), dicomData.getColumns(), dicomData.getRows(), dicomData.getSamplesPerPixel()));

                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void closeDicomImageViewer() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.secondaryColor));
        binding.dicomImageContainer.setVisibility(View.GONE);
    }

    private void setupDicomFile(String dicomFilePathOrUri, boolean isUri) {
        showLoadingDialog();

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                File dicomFile = null;

                if (isUri) {
                    dicomData = retrieveDicomFileData(dicomFilePathOrUri);
                } else {
                    dicomFile = new File(dicomFilePathOrUri);
                    dicomData = retrieveDicomFileData(dicomFile);
                }


                frames = new ArrayList<>();

                try {
                    for (int i = 0; i < dicomData.getNumberOfFrames(); i++) {
                        frames.add(extractSingleFrame(dicomData.getPixelData(), i, dicomData.getRows(), dicomData.getColumns(), dicomData.getSamplesPerPixel()));
                    }
                } catch (Exception e) {

                    e.printStackTrace();
                }


                runOnUiThread(() -> {
                    try {
                        binding.fileName.setText(dicomData.getPatientId().concat(".dcm"));
                        populateTableWithDicomData(binding.tableLayout, dicomData);
                        binding.imageView.setImageBitmap(convertByteArrayToBitmap(frames.get(0), dicomData.getColumns(), dicomData.getRows(), dicomData.getSamplesPerPixel()));
                        dismissLoadingDialog();
                    } catch (Exception e) {
                        Toast.makeText(DicomDetailsActivity.this, "Something wrong with Dicom PixelData", Toast.LENGTH_LONG).show();
                        dismissLoadingDialog();
                    }
                });
            }
        });
    }

    private void populateTableWithDicomData(TableLayout tableLayout, DicomData dicomData) {
        addRowToTable(tableLayout, "Patient ID", dicomData.getPatientId());
        addRowToTable(tableLayout, "Patient Name", dicomData.getPatientName());
        addRowToTable(tableLayout, "Patient Age", String.valueOf(dicomData.getPatientAge()));
        addRowToTable(tableLayout, "Patient Sex", dicomData.getPatientSex());
        addRowToTable(tableLayout, "Instance Creation Date", dicomData.getInstanceCreationDate());
        addRowToTable(tableLayout, "Instance Creation Time", dicomData.getInstanceCreationTime());
        addRowToTable(tableLayout, "Study Date", dicomData.getStudyDate());
        addRowToTable(tableLayout, "Study Time", dicomData.getStudyTime());
        addRowToTable(tableLayout, "Image Comments", dicomData.getImageComments());
        addRowToTable(tableLayout, "Samples Per Pixel", String.valueOf(dicomData.getSamplesPerPixel()));
        addRowToTable(tableLayout, "Photometric Interpretation", dicomData.getPhotometricInterpretation());
        addRowToTable(tableLayout, "Planar Configuration", String.valueOf(dicomData.getPlanarConfiguration()));
        addRowToTable(tableLayout, "Number Of Frames", String.valueOf(dicomData.getNumberOfFrames()));
        addRowToTable(tableLayout, "Rows", String.valueOf(dicomData.getRows()));
        addRowToTable(tableLayout, "Columns", String.valueOf(dicomData.getColumns()));
        addRowToTable(tableLayout, "Bits Allocated", String.valueOf(dicomData.getBitsAllocated()));
        addRowToTable(tableLayout, "Bits Stored", String.valueOf(dicomData.getBitsStored()));
        addRowToTable(tableLayout, "High Bit", String.valueOf(dicomData.getHighBit()));
        addRowToTable(tableLayout, "Pixel Representation", String.valueOf(dicomData.getPixelRepresentation()));
        // Add other DICOM attributes as needed

    }

    private void addRowToTable(TableLayout table, String label, String value) {
        TableRow row = new TableRow(this);
        row.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));

        // Define a common layout parameter for cells
        TableRow.LayoutParams cellParams = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);
        cellParams.setMargins(1, 1, 1, 1); // Add margins for border effect

        // Adding Label TextView
        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setBackground(ContextCompat.getDrawable(this, R.drawable.table_cell_border));
        labelView.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        labelView.setPadding(8, 8, 8, 8);
        row.addView(labelView, cellParams);

        // Adding Value TextView
        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setBackground(ContextCompat.getDrawable(this, R.drawable.table_cell_border));
        valueView.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        valueView.setPadding(8, 8, 8, 8);
        row.addView(valueView, cellParams);

        table.addView(row);
    }


    public DicomData retrieveDicomFileData(File dicomFile) {
        try {
            DicomInputStream dcmInputStream;
            dcmInputStream = new DicomInputStream(dicomFile);

            Attributes attrs = dcmInputStream.readDataset(-1, -1);

            // Extracting other DICOM attributes
            String patientId = attrs.getString(Tag.PatientID, "");
            String patientName = attrs.getString(Tag.PatientName, "");
            String patientAge = attrs.getString(Tag.PatientAge, "0");
            String patientSex = attrs.getString(Tag.PatientSex, "");
            String instanceCreationDate = attrs.getString(Tag.InstanceCreationDate, "");
            String instanceCreationTime = attrs.getString(Tag.InstanceCreationTime, "");
            String studyDate = attrs.getString(Tag.StudyDate, "");
            String studyTime = attrs.getString(Tag.StudyTime, "");
            String imageComments = attrs.getString(Tag.ImageComments, "");
            String photometricInterpretation = attrs.getString(Tag.PhotometricInterpretation);

            int planarConfiguration = attrs.getInt(Tag.PlanarConfiguration, 0);
            int bitsAllocated = attrs.getInt(Tag.BitsAllocated, 8);
            int bitsStored = attrs.getInt(Tag.BitsStored, 8);
            int highBit = attrs.getInt(Tag.HighBit, 7);
            int samplesPerPixel = attrs.getInt(Tag.SamplesPerPixel, 3);
            int pixelRepresentation = attrs.getInt(Tag.PixelRepresentation, 0);
            int numberOfFrames = attrs.getInt(Tag.NumberOfFrames, 0);
            int row = attrs.getInt(Tag.Rows, 1);
            int columns = attrs.getInt(Tag.Columns, 1);

            byte[] pixelData = null;
            try {
                pixelData = attrs.getSafeBytes(Tag.PixelData);
            } catch (Exception e) {

            }


            SafeClose.close(dcmInputStream);

            return new DicomData(patientId, patientName, patientAge, patientSex, instanceCreationDate, instanceCreationTime, studyDate, studyTime, imageComments, samplesPerPixel, photometricInterpretation, planarConfiguration, numberOfFrames, row, columns, bitsAllocated, bitsStored, highBit, pixelRepresentation, pixelData);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public DicomData retrieveDicomFileData(String uri) {
        try {
            DicomInputStream dcmInputStream;

            dcmInputStream = new DicomInputStream(Objects.requireNonNull(getContentResolver().openInputStream(Uri.parse(uri))));

            Attributes attrs = dcmInputStream.readDataset(-1, -1);

            // Extracting other DICOM attributes
            String patientId = attrs.getString(Tag.PatientID, "");
            String patientName = attrs.getString(Tag.PatientName, "");
            String patientAge = attrs.getString(Tag.PatientAge, "0");
            String patientSex = attrs.getString(Tag.PatientSex, "");
            String instanceCreationDate = attrs.getString(Tag.InstanceCreationDate, "");
            String instanceCreationTime = attrs.getString(Tag.InstanceCreationTime, "");
            String studyDate = attrs.getString(Tag.StudyDate, "");
            String studyTime = attrs.getString(Tag.StudyTime, "");
            String imageComments = attrs.getString(Tag.ImageComments, "");
            String photometricInterpretation = attrs.getString(Tag.PhotometricInterpretation);

            int planarConfiguration = attrs.getInt(Tag.PlanarConfiguration, 0);
            int bitsAllocated = attrs.getInt(Tag.BitsAllocated, 8);
            int bitsStored = attrs.getInt(Tag.BitsStored, 8);
            int highBit = attrs.getInt(Tag.HighBit, 7);
            int samplesPerPixel = attrs.getInt(Tag.SamplesPerPixel, 3);
            int pixelRepresentation = attrs.getInt(Tag.PixelRepresentation, 0);
            int numberOfFrames = attrs.getInt(Tag.NumberOfFrames, 0);
            int row = attrs.getInt(Tag.Rows, 1);
            int columns = attrs.getInt(Tag.Columns, 1);
            byte[] pixelData = null;
            try {
                pixelData = attrs.getSafeBytes(Tag.PixelData);
            } catch (Exception e) {
//                pixelData = getPixelDataFromDicom();
            }

            SafeClose.close(dcmInputStream);

            return new DicomData(patientId, patientName, patientAge, patientSex, instanceCreationDate, instanceCreationTime, studyDate, studyTime, imageComments, samplesPerPixel, photometricInterpretation, planarConfiguration, numberOfFrames, row, columns, bitsAllocated, bitsStored, highBit, pixelRepresentation, pixelData);

        } catch (Exception e) {
            finish();
        }
        return null;
    }

    public byte[] getPixelDataFromDicom(String filePath) {
        try {
            CodecFactory.setMaximumImageSize(8000, 8000);
            DataSet dataSet = CodecFactory.load(filePath); // Load the DICOM dataset

            long numberOfFrames = dataSet.getUnsignedLong(new TagId(0x0028, 0x0008), 0, 1);
            Image image = dataSet.getImageApplyModalityTransform(0); // Get the first frame

            // Use a DrawBitmap to get the raw bytes
            DrawBitmap drawBitmap = new DrawBitmap(new TransformsChain());
            Memory memory = drawBitmap.getBitmap(image, drawBitmapType_t.drawBitmapRGBA, 4);

            // Convert Memory to byte array
            byte[] buffer = new byte[(int) memory.size()];
            memory.data(buffer);

            return buffer;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] extractSingleFrame(byte[] pixelData, int frameIndex, int rows, int columns, int samplesPerPixel) {
        int frameSize = rows * columns * samplesPerPixel; // Size of one frame in bytes
        int start = frameIndex * frameSize; // Starting point of the frame in the pixel data array

        if (start + frameSize > pixelData.length) {
            throw new IllegalArgumentException("Frame index is out of bounds.");
        }

        byte[] framePixelData = new byte[frameSize];
        System.arraycopy(pixelData, start, framePixelData, 0, frameSize);
        return framePixelData;
    }

    Bitmap convertByteArrayToBitmap(byte[] pixelData, int width, int height, int samplesPerPixel) {
        if (samplesPerPixel == 3) {
            return convertRGBByteArrayToARGBIntArray(pixelData, width, height);
        } else {
            return convertGrayscaleByteArrayToBitmap(pixelData, width, height);
        }
    }

    public Bitmap convertGrayscaleByteArrayToBitmap(byte[] pixelData, int width, int height) {
        int[] intPixels = new int[pixelData.length];
        for (int i = 0; i < pixelData.length; i++) {
            // Assuming grayscale, so making the red, green, and blue values the same
            int pixel = pixelData[i] & 0xff; // Convert byte to unsigned
            intPixels[i] = 0xff000000 | (pixel << 16) | (pixel << 8) | pixel; // ARGB
        }

        return Bitmap.createBitmap(intPixels, width, height, Bitmap.Config.ARGB_8888);
    }

    public Bitmap convertRGBByteArrayToARGBIntArray(byte[] rgbData, int width, int height) {
        int[] argbPixels = new int[width * height];
        for (int i = 0; i < argbPixels.length; i++) {
            int r = rgbData[i * 3] & 0xFF;
            int g = rgbData[i * 3 + 1] & 0xFF;
            int b = rgbData[i * 3 + 2] & 0xFF;
            argbPixels[i] = 0xFF000000 | (r << 16) | (g << 8) | b; // ARGB
        }
        return Bitmap.createBitmap(argbPixels, width, height, Bitmap.Config.ARGB_8888);
    }

    private void showLoadingDialog() {
        if (loadingDialog == null) {
            loadingDialog = new Dialog(this);
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null);
            ((TextView) view.findViewById(R.id.tvStatus)).setText("Loading...");
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

    @Override
    public void onBackPressed() {
        if (binding.dicomImageContainer.getVisibility() == View.VISIBLE) {
            closeDicomImageViewer();
        } else {
            super.onBackPressed();
        }
    }

    private void shareDicomFile(String dicomFilePath) {
        File dicomFile = new File(dicomFilePath);
        if (!dicomFile.exists()) {
            Toast.makeText(this, "Something wrong with dicom file", Toast.LENGTH_SHORT).show();

            return;
        }

        Uri fileUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", dicomFile);

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.setType("application/dicom"); // Set MIME type for DICOM
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, "Share DICOM File"));
    }

}