package com.jamesmobiledev.dicom

import android.app.Application
import timber.log.Timber
import timber.log.Timber.DebugTree


class DicomApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }

        System.loadLibrary("imebra_lib")
    }
}



