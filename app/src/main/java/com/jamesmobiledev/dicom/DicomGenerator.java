package com.jamesmobiledev.dicom;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.jamesmobiledev.dicom.model.DicomData;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.dcm4che3.io.DicomOutputStream;
import org.dcm4che3.util.UIDUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class DicomGenerator {
    private Context context;

    public DicomGenerator(Context context) {
        this.context = context;

    }

    public void convertImagesToDicom(DicomData dicomData, List<Uri> imageUris) {
        try {
            Attributes dicom = createDicomHeader(getBitmapFromUri(imageUris.get(0)), dicomData, imageUris.size());
            Attributes fmi = createDicomFmi(dicom);

            byte[] pixelData = null;

            for (int i = 0; i < imageUris.size(); i++) {
                Bitmap bitmap = getBitmapFromUri(imageUris.get(i));

                byte[] currentImageData;
                if (dicomData.getImageColorInfo().equals("RGB")) {
                    currentImageData = convertBitmapToRGBBytes(bitmap);
                } else {
                    currentImageData = convertBitmapToGrayscaleBytes(bitmap);
                }

                if (pixelData == null) {
                    pixelData = currentImageData;
                } else {
                    pixelData = appendByteArrays(pixelData, currentImageData);
                }
            }

            dicom.setBytes(Tag.PixelData, VR.OB, pixelData);

            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(), "james7.dcm");

            File dcmFile = new File(new File(context.getExternalFilesDir(null), "MiruDicom").getAbsolutePath() + "/" + dicomData.getPatientId() + ".dcm");

            DicomOutputStream dos = new DicomOutputStream(dcmFile);
            dos.writeDataset(fmi, dicom);
            dos.close();

        } catch (Exception e) {
            Log.d("@@@", "DicomGenerator: " + e.toString());
            e.printStackTrace();
        }
    }

    private byte[] appendByteArrays(byte[] arrayA, byte[] arrayB) {
        byte[] result = new byte[arrayA.length + arrayB.length];
        System.arraycopy(arrayA, 0, result, 0, arrayA.length);
        System.arraycopy(arrayB, 0, result, arrayA.length, arrayB.length);
        return result;
    }


    private Attributes createDicomFmi(Attributes dicom) {
        Attributes fmi = new Attributes();
        fmi.setString(Tag.ImplementationVersionName, VR.SH, "DCM4CHE3");
        fmi.setString(Tag.ImplementationClassUID, VR.UI, UIDUtils.createUID());
        fmi.setString(Tag.TransferSyntaxUID, VR.UI, UID.ExplicitVRLittleEndian);
        fmi.setString(Tag.MediaStorageSOPInstanceUID, VR.UI, UIDUtils.createUID());
        fmi.setString(Tag.FileMetaInformationVersion, VR.OB, "0", "1");
        fmi.setInt(Tag.FileMetaInformationGroupLength, VR.UL, dicom.size() + fmi.size());
        return fmi;
    }

    private Date convertTIme(String imageTime) {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

        try {
            return timeFormat.parse(imageTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Date convertDate(String imageDate) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        try {
            return dateFormat.parse(imageDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Bitmap getBitmapFromUri(Uri uri) {
        Bitmap bitmap = null;
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                bitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();

                // Adjust orientation
                int orientation = getExifOrientation(uri, context);
                bitmap = rotateBitmap(bitmap, orientation);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    private Bitmap rotateBitmap(Bitmap bitmap, int orientation) {
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private byte[] convertBitmapToRGBBytes(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        byte[] rgbData = new byte[width * height * 3]; // 3 bytes per pixel (R, G, B)

        for (int i = 0; i < pixels.length; i++) {
            rgbData[i * 3] = (byte) ((pixels[i] >> 16) & 0xFF);     // Red
            rgbData[i * 3 + 1] = (byte) ((pixels[i] >> 8) & 0xFF); // Green
            rgbData[i * 3 + 2] = (byte) (pixels[i] & 0xFF);        // Blue
        }
        return rgbData;
    }

    private byte[] convertBitmapToGrayscaleBytes(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        byte[] grayscaleData = new byte[width * height];  // Buffer for grayscale data
        for (int i = 0; i < pixels.length; i++) {
            int argb = pixels[i];
            int r = (argb >> 16) & 0xFF;
            int g = (argb >> 8) & 0xFF;
            int b = argb & 0xFF;
            int grayscale = (r + g + b) / 3;  // Simple average for grayscale
            grayscaleData[i] = (byte) grayscale;
        }

        return grayscaleData;
    }

    private int getExifOrientation(Uri uri, Context context) {
        int orientation = ExifInterface.ORIENTATION_NORMAL;
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                ExifInterface exif = new ExifInterface(inputStream);
                orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                inputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return orientation;
    }

    private Attributes createDicomHeader(Bitmap jpegBitmap, DicomData dicomData, int frameNumber) {
        int colorComponents = dicomData.getImageColorInfo().equals("RGB") ? 3 : 1;
        int bitsPerPixel = 8;
        int bitsAllocated = bitsPerPixel;
        int samplesPerPixel = colorComponents;

        // Create a new DICOM dataset (Attributes) for storing information
        Attributes dicom = new Attributes();

        // Add patient related information to the DICOM dataset
        dicom.setString(Tag.PatientID, VR.LO, dicomData.getPatientId());
        dicom.setString(Tag.PatientName, VR.PN, dicomData.getPatientName());
        dicom.setString(Tag.PatientAge, VR.AS, String.valueOf(dicomData.getPatientAge()));
        dicom.setString(Tag.PatientSex, VR.CS, dicomData.getPatientSex());

        dicom.setDate(Tag.StudyDate, VR.DA, convertDate(dicomData.getImageDate()));
        dicom.setDate(Tag.StudyTime, VR.TM, convertTIme(dicomData.getImageTime()));
        dicom.setString(Tag.ImageComments, VR.LT, dicomData.getImageComments());


        // Set various DICOM attributes
        dicom.setString(Tag.PhotometricInterpretation, VR.CS, dicomData.getImageColorInfo().equals("RGB") ? "RGB" : "MONOCHROME2");
        dicom.setInt(Tag.SamplesPerPixel, VR.US, samplesPerPixel);
        dicom.setInt(Tag.Rows, VR.US, jpegBitmap.getHeight());
        dicom.setInt(Tag.Columns, VR.US, jpegBitmap.getWidth());
        dicom.setInt(Tag.BitsAllocated, VR.US, bitsAllocated);
        dicom.setInt(Tag.BitsStored, VR.US, bitsAllocated);
        dicom.setInt(Tag.HighBit, VR.US, bitsAllocated - 1);
        dicom.setInt(Tag.PixelRepresentation, VR.US, 0);
        dicom.setInt(Tag.NumberOfFrames, VR.IS, frameNumber);
        dicom.setInt(Tag.PlanarConfiguration, VR.US, 0);
        dicom.setDate(Tag.InstanceCreationDate, VR.DA, new Date());
        dicom.setDate(Tag.InstanceCreationTime, VR.TM, new Date());

        return dicom;
    }
}
