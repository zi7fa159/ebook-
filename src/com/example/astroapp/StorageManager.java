package com.example.astroapp;

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.DngCreator;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StorageManager {
    private File sessionDir;
    private Context context;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());

    public StorageManager(Context context) {
        this.context = context;
    }

    public void newSession() {
        File baseDir = context.getExternalFilesDir(null);
        if (baseDir == null) baseDir = context.getFilesDir();

        File astroDir = new File(baseDir, "AstroApp");
        if (!astroDir.exists()) astroDir.mkdirs();

        sessionDir = new File(astroDir, "Session_" + sdf.format(new Date()));
        if (!sessionDir.exists()) sessionDir.mkdirs();
    }

    public void saveDng(Image image, CameraCharacteristics characteristics, TotalCaptureResult result, int frameIndex, long exposureNs, int iso) {
        if (sessionDir == null) newSession();

        File file = new File(sessionDir, String.format("frame_%04d.dng", frameIndex));
        DngCreator dngCreator = new DngCreator(characteristics, result);

        saveMetadata(frameIndex, exposureNs, iso);

        try (FileOutputStream output = new FileOutputStream(file)) {
            dngCreator.writeImage(output, image);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            dngCreator.close();
        }
    }

    private void saveMetadata(int frameIndex, long exposureNs, int iso) {
        File metaFile = new File(sessionDir, "metadata.json");
        String entry = String.format("{\"frame\": %d, \"exposure_ns\": %d, \"iso\": %d, \"time\": %d}\n",
            frameIndex, exposureNs, iso, System.currentTimeMillis());

        try (FileOutputStream fos = new FileOutputStream(metaFile, true)) {
            fos.write(entry.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
