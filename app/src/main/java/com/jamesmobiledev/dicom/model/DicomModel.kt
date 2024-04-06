package com.jamesmobiledev.dicom.model

import android.graphics.Bitmap

data class DicomModel(
    val fileName: String,
    val patientName: String,
    val patientBirthDate: String,
    val institution: String,
    val station: String,
    val manufacturer: String,
    val manufacturerModelName: String,
    val description: String,
    val seriesDescription: String,
    val studyDate: String,
    val rows: Int,
    val columns: Int,
    val winCenter: Float,
    val winWidth: Float,
    val pixelData: ByteArray?,
    var bitmap: Bitmap? = null // Add a nullable Bitmap property
)
