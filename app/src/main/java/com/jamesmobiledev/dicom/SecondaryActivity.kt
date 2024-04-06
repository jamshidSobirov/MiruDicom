package com.jamesmobiledev.dicom

import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.jamesmobiledev.dicom.databinding.ActivitySecondaryBinding
import com.jamesmobiledev.dicom.model.DicomModel
import org.dcm4che3.android.RasterUtil
import org.dcm4che3.android.imageio.dicom.DicomImageReader
import org.dcm4che3.data.Attributes
import org.dcm4che3.data.Tag
import org.dcm4che3.data.VR
import org.dcm4che3.io.DicomInputStream
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream


class SecondaryActivity : AppCompatActivity() {

    lateinit var binding: ActivitySecondaryBinding
    var number = 1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySecondaryBinding.inflate(layoutInflater)
        setContentView(binding.root)



//        Log.d("@@@", "onCreate: ${a.byteArr.size}")

//        btnLoadClicked("fileName")

//        binding.iv.setImageBitmap(BitmapFactory.decodeByteArray(a.byteArr, 0, a.byteArr.size))

//        jpg2dcm(sourceFile, destinationFile)
//        convertJpgsToDcm(destinationFile, listOf(sourceFile))

//        try {
//            val f = Mpeg2Dicom()
//            val jpgFiles = arrayOf(sourceFile)
//            val mpegFile = File(
//                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path,
//                "sample2.mpeg"
//            )
//            f.encodeMultiframe(mpegFile, destinationFile)
//        } catch (e: Exception) {
//            // Print exceptions
//            Log.d("@@@", "onCreate: ${e.message}")
//            e.printStackTrace();
//        }

    }


    fun next(view: View) {
        binding.tvName.text = ""
        binding.tvBirthday.text = ""
        binding.tvInstitution.text = ""
        binding.tvStation.text = ""
        binding.tvStudyDescription.text = ""
        binding.tvManufacturer.text = ""
        binding.tvManufacturerModelName.text = ""
        binding.tvStudyDate.text = ""
        binding.tvSeriesDescription.text = ""
        binding.iv.setImageResource(R.drawable.github_logo)

        val fileName = "test$number.dcm"
        binding.tvPicture.text = fileName

        btnLoadClicked(fileName)

        number++
        if (number === 15) number = 1

    }

    private fun btnLoadClicked(fileName: String) {
        val testFileName = this.cacheDir.absolutePath + "/" + fileName
        var file = File(testFileName)
        if (file.exists()) file.delete()

        //temp
        file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path,
            "james7.dcm"
        )
        //temp
        val inputStream: InputStream

        try {
            inputStream = assets.open(fileName)
            copyFile(inputStream, file)
            populateViewsFromDicomModel(retrieveDicomFileData(testFileName, fileName))
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun copyFile(inputStream: InputStream?, dstFile: File?) {
        try {
            val bis = BufferedInputStream(inputStream)
            val bos = BufferedOutputStream(
                FileOutputStream(dstFile), 1024
            )
            val buf = ByteArray(1024)
            var c = 0
            c = bis.read(buf)
            while (c > 0) {
                bos.write(buf, 0, c)
                c = bis.read(buf)
            }
            bis.close()
            bos.close()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }


    private fun retrieveDicomFileData(filePath: String?, fileName: String): DicomModel? {
        val dr = DicomImageReader()
        try {
            val file = File(filePath)
            val dcmInputStream = DicomInputStream(file)
            val attrs: Attributes = dcmInputStream.readDataset(-1, -1)

            Timber.tag("@@@").d(attrs.toString())

            val row: Int = attrs.getInt(Tag.Rows, 1)
            val columns: Int = attrs.getInt(Tag.Columns, 1)

            val winCenter: Float = attrs.getFloat(Tag.WindowCenter, 1f)
            val winWidth: Float = attrs.getFloat(Tag.WindowWidth, 1f)

            val pixelData: ByteArray? = attrs.getSafeBytes(Tag.PixelData)

            attrs.setString(Tag.SpecificCharacterSet, VR.CS, "GB18030")

            val patientName: String = attrs.getString(Tag.PatientName, "")
            val patientBirthDate: String = attrs.getString(Tag.PatientBirthDate, "")
            val institution: String = attrs.getString(Tag.InstitutionName, "")
            val station: String = attrs.getString(Tag.StationName, "")
            val manufacturer: String = attrs.getString(Tag.Manufacturer, "")
            val manufacturerModelName: String = attrs.getString(Tag.ManufacturerModelName, "")
            val description: String = attrs.getString(Tag.StudyDescription, "")
            val seriesDescription: String = attrs.getString(Tag.SeriesDescription, "")
            val studyDate: String = attrs.getString(Tag.StudyDate, "")

            // Create a DicomModel object with the extracted information
            val dicomModel = DicomModel(
                fileName,
                patientName,
                patientBirthDate,
                institution,
                station,
                manufacturer,
                manufacturerModelName,
                description,
                seriesDescription,
                studyDate,
                row,
                columns,
                winCenter,
                winWidth,
                pixelData
            )

            dr.open(file)

            // Assuming dr.applyWindowCenter returns a Raster object
            val raster = dr.applyWindowCenter(0, winWidth.toInt(), winCenter.toInt())

            // Assuming RasterUtil.gray8ToBitmap returns a Bitmap object
            val bmp = RasterUtil.gray8ToBitmap(columns, row, raster.byteData)

            // Set the Bitmap in the DicomModel
            dicomModel.bitmap = bmp
            Log.d("@@@", "retrieveDicomFileData: ${bmp.width}")

            return dicomModel
        } catch (e: Exception) {
            Log.e("@@@", "" + e.message)
        }
        return null
    }


    private fun populateViewsFromDicomModel(dicomModel: DicomModel?) {
        if (dicomModel != null) {
            binding.tvName.text = "Patient Name: ${dicomModel.patientName}"
            binding.tvBirthday.text = "Patient Birth Date: ${dicomModel.patientBirthDate}"
            binding.tvInstitution.text = "Institution: ${dicomModel.institution}"
            binding.tvStation.text = "Station: ${dicomModel.station}"
            binding.tvStudyDescription.text = "Study Description: ${dicomModel.description}"
            binding.tvManufacturer.text = "Manufacturer: ${dicomModel.manufacturer}"
            binding.tvManufacturerModelName.text =
                "Manufacturer Model Name: ${dicomModel.manufacturerModelName}"
            binding.tvStudyDate.text = "Study Date: ${dicomModel.studyDate}"
            binding.tvSeriesDescription.text = "Series Description: ${dicomModel.seriesDescription}"

            dicomModel.bitmap?.let {
                binding.iv.setImageBitmap(it)
            } ?: run {
                // Set a default image if bitmap is null
                binding.iv.setImageResource(R.drawable.github_logo)
            }

            binding.tvPicture.text = "File Name: ${dicomModel.fileName}"
        } else {
            // Clear all views if the DicomModel is null
            binding.tvName.text = ""
            binding.tvBirthday.text = ""
            binding.tvInstitution.text = ""
            binding.tvStation.text = ""
            binding.tvStudyDescription.text = ""
            binding.tvManufacturer.text = ""
            binding.tvManufacturerModelName.text = ""
            binding.tvStudyDate.text = ""
            binding.tvSeriesDescription.text = ""
            binding.iv.setImageResource(R.drawable.github_logo)
            binding.tvPicture.text = ""
        }
    }


}