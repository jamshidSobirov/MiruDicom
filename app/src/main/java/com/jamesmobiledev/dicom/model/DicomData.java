package com.jamesmobiledev.dicom.model;

public class DicomData {
    private String patientId;

    private String doctorName;
    private int doctorAge;
    private String doctorSex;
    private String patientName;
    private String patientAge;
    private String patientSex;
    private String imageDate;
    private String imageTime;
    private String imageColorInfo;
    private String imageComments;
    // Add other fields as necessary

    private String instanceCreationDate;
    private String instanceCreationTime;
    private String studyDate;
    private String studyTime;
    private int samplesPerPixel;
    private String photometricInterpretation;
    private int planarConfiguration;
    private int numberOfFrames;
    private int rows;
    private int columns;
    private int bitsAllocated;
    private int bitsStored;
    private int highBit;
    private int pixelRepresentation;
    private byte[] pixelData;

    public DicomData() {

    }

    public DicomData(String patientId, String patientName, String  patientAge, String patientSex, String instanceCreationDate, String instanceCreationTime, String studyDate, String studyTime, String imageComments, int samplesPerPixel, String photometricInterpretation, int planarConfiguration, int numberOfFrames, int rows, int columns, int bitsAllocated, int bitsStored, int highBit, int pixelRepresentation, byte[] pixelData) {
        this.patientId = patientId;
        this.patientName = patientName;
        this.patientAge = patientAge;
        this.patientSex = patientSex;
        this.instanceCreationDate = instanceCreationDate;
        this.instanceCreationTime = instanceCreationTime;
        this.studyDate = studyDate;
        this.studyTime = studyTime;
        this.imageComments = imageComments;
        this.samplesPerPixel = samplesPerPixel;
        this.photometricInterpretation = photometricInterpretation;
        this.planarConfiguration = planarConfiguration;
        this.numberOfFrames = numberOfFrames;
        this.rows = rows;
        this.columns = columns;
        this.bitsAllocated = bitsAllocated;
        this.bitsStored = bitsStored;
        this.highBit = highBit;
        this.pixelRepresentation = pixelRepresentation;
        this.pixelData = pixelData;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public int getDoctorAge() {
        return doctorAge;
    }

    public String getDoctorSex() {
        return doctorSex;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getPatientAge() {
        return patientAge;
    }

    public String getPatientSex() {
        return patientSex;
    }

    public String getImageDate() {
        return imageDate;
    }

    public String getImageTime() {
        return imageTime;
    }

    public String getImageColorInfo() {
        return imageColorInfo;
    }

    public String getImageComments() {
        return imageComments;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public void setDoctorAge(int doctorAge) {
        this.doctorAge = doctorAge;
    }

    public void setDoctorSex(String doctorSex) {
        this.doctorSex = doctorSex;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public void setPatientAge(String patientAge) {
        this.patientAge = patientAge;
    }

    public void setPatientSex(String patientSex) {
        this.patientSex = patientSex;
    }

    public void setImageDate(String imageDate) {
        this.imageDate = imageDate;
    }

    public void setImageTime(String imageTime) {
        this.imageTime = imageTime;
    }

    public void setImageColorInfo(String imageColorInfo) {
        this.imageColorInfo = imageColorInfo;
    }

    public void setImageComments(String imageComments) {
        this.imageComments = imageComments;
    }

    public DicomData(String doctorName, int doctorAge, String doctorSex, String patientName, String patientAge, String patientSex, String imageDate, String imageTime, String imageColorInfo, String imageComments) {
        this.doctorName = doctorName;
        this.doctorAge = doctorAge;
        this.doctorSex = doctorSex;
        this.patientName = patientName;
        this.patientAge = patientAge;
        this.patientSex = patientSex;
        this.imageDate = imageDate;
        this.imageTime = imageTime;
        this.imageColorInfo = imageColorInfo;
        this.imageComments = imageComments;
    }

    public String getInstanceCreationDate() {
        return instanceCreationDate;
    }

    public String getInstanceCreationTime() {
        return instanceCreationTime;
    }

    public String getStudyDate() {
        return studyDate;
    }

    public String getStudyTime() {
        return studyTime;
    }

    public int getSamplesPerPixel() {
        return samplesPerPixel;
    }

    public String getPhotometricInterpretation() {
        return photometricInterpretation;
    }

    public int getPlanarConfiguration() {
        return planarConfiguration;
    }

    public int getNumberOfFrames() {
        return numberOfFrames;
    }

    public int getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
    }

    public int getBitsAllocated() {
        return bitsAllocated;
    }

    public int getBitsStored() {
        return bitsStored;
    }

    public int getHighBit() {
        return highBit;
    }

    public int getPixelRepresentation() {
        return pixelRepresentation;
    }

    public byte[] getPixelData() {
        return pixelData;
    }
}

