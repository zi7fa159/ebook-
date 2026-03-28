package com.astroapp;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.DngCreator;
import android.media.Image;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StorageManager {
    private static final String TAG = "StorageManager";
    private File sessionFolder;

    public StorageManager() {
        createSessionFolder();
    }

    private void createSessionFolder() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File root = new File(Environment.getExternalStorageDirectory(), "AstroApp");
        sessionFolder = new File(root, "Session_" + timestamp);
        if (!sessionFolder.exists()) {
            if (!sessionFolder.mkdirs()) {
                Log.e(TAG, "Failed to create session folder");
            }
        }
    }

    public void saveFrame(Image image, int frameNumber, CaptureResult result, CameraCharacteristics characteristics, int iso, long exposureNs) {
        String frameName = String.format(Locale.US, "frame_%04d.dng", frameNumber);
        File dngFile = new File(sessionFolder, frameName);

        try (FileOutputStream output = new FileOutputStream(dngFile)) {
            DngCreator dngCreator = new DngCreator(characteristics, result);
            dngCreator.writeImage(output, image);
            Log.d(TAG, "Saved: " + dngFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Error saving frame: " + frameNumber, e);
        } finally {
            image.close();
        }

        saveMetadata(frameNumber, iso, exposureNs);
    }

    private void saveMetadata(int frameNumber, int iso, long exposureNs) {
        File metaFile = new File(sessionFolder, "metadata.json");
        boolean append = metaFile.exists();

        try (FileWriter writer = new FileWriter(metaFile, true)) {
            if (!append) writer.write("[\n");
            else writer.write(",\n");

            String json = String.format(Locale.US,
                "{\"frame\": %d, \"iso\": %d, \"exposure_ns\": %d, \"timestamp\": %d}",
                frameNumber, iso, exposureNs, System.currentTimeMillis());

            writer.write(json);
        } catch (IOException e) {
            Log.e(TAG, "Error saving metadata", e);
        }
    }

    public void finalizeSession() {
        File metaFile = new File(sessionFolder, "metadata.json");
        if (metaFile.exists()) {
            try (FileWriter writer = new FileWriter(metaFile, true)) {
                writer.write("\n]");
            } catch (IOException e) {
                Log.e(TAG, "Error finalizing metadata", e);
            }
        }
    }
}
