package com.example.a2livestacker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.Surface
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.nio.ByteBuffer

class MainActivity : AppCompatActivity() {

    private lateinit var previewImage: ImageView
    private lateinit var statusText: TextView
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isStacking = false
    private var refreshRateS = 3

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var cameraHandler: Handler? = null
    private var cameraThread: HandlerThread? = null

    private var exposureMs: Long = 100
    private var iso: Int = 800
    private var targetWidth = 1280
    private var targetHeight = 720

    // JNI functions exactly as requested
    external fun initStacker(width: Int, height: Int)
    external fun addFrame(byteBuffer: ByteBuffer)
    external fun getPreview(): ByteArray?
    external fun resetStack()

    companion object {
        init {
            System.loadLibrary("native-lib")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewImage = findViewById(R.id.previewImage)
        statusText = findViewById(R.id.statusText)

        setupSpinners()

        findViewById<Button>(R.id.startBtn).setOnClickListener {
            if (!isStacking) {
                if (checkCameraPermission()) {
                    startStacking()
                } else {
                    requestCameraPermission()
                }
            } else {
                stopStacking()
            }
        }

        findViewById<Button>(R.id.resetBtn).setOnClickListener {
            resetStack()
            previewImage.setImageDrawable(null)
            statusText.text = "Status: Reset"
        }

        findViewById<SeekBar>(R.id.refreshSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                refreshRateS = if (progress < 1) 1 else progress
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        findViewById<SeekBar>(R.id.exposureSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                exposureMs = progress.toLong()
                updateCameraSettings()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        findViewById<SeekBar>(R.id.isoSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                iso = progress
                updateCameraSettings()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupSpinners() {
        val methods = arrayOf("DSO", "Planetary", "Dynamic")
        findViewById<Spinner>(R.id.methodSpinner).adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, methods)

        val alignments = arrayOf("FFT", "None")
        findViewById<Spinner>(R.id.alignmentSpinner).adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, alignments)

        val sizes = arrayOf("1280x720", "1920x1080", "640x480")
        val sizeSpinner = findViewById<Spinner>(R.id.sizeSpinner)
        sizeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sizes)
        sizeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val s = sizes[position].split("x")
                targetWidth = s[0].toInt()
                targetHeight = s[1].toInt()
                if (isStacking) {
                    stopStacking()
                    startStacking()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun startStacking() {
        isStacking = true
        findViewById<Button>(R.id.startBtn).text = "Stop"
        initStacker(targetWidth, targetHeight)
        statusText.text = "Status: Opening Camera..."
        openCamera()
        startPreviewLoop()
    }

    private fun stopStacking() {
        isStacking = false
        findViewById<Button>(R.id.startBtn).text = "Start"
        statusText.text = "Status: Stopped"
        closeCamera()
    }

    private fun startPreviewLoop() {
        mainHandler.post(object : Runnable {
            override fun run() {
                if (!isStacking) return
                val jpegData = getPreview()
                if (jpegData != null) {
                    val bitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.size)
                    previewImage.setImageBitmap(bitmap)
                }
                mainHandler.postDelayed(this, (refreshRateS * 1000).toLong())
            }
        })
    }

    private fun openCamera() {
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraId = manager.cameraIdList[0]
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return

            cameraThread = HandlerThread("CameraThread").also { it.start() }
            cameraHandler = Handler(cameraThread!!.looper)

            imageReader = ImageReader.newInstance(targetWidth, targetHeight, ImageFormat.YUV_420_888, 2)
            imageReader?.setOnImageAvailableListener({ reader ->
                val image = try { reader.acquireLatestImage() } catch (e: Exception) { null }
                if (image != null) {
                    // Pass the Y plane for simplicity and stability in this minimal version
                    val buffer = image.planes[0].buffer
                    addFrame(buffer)
                    image.close()
                }
            }, cameraHandler)

            manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    createCaptureSession()
                }
                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                }
                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    cameraDevice = null
                }
            }, cameraHandler)
        } catch (e: CameraAccessException) {
            Log.e("A2LiveStacker", "Camera access exception", e)
        }
    }

    private fun createCaptureSession() {
        val surface = imageReader?.surface ?: return
        cameraDevice?.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                captureSession = session
                updateCameraSettings()
                mainHandler.post { statusText.text = "Status: Stacking active" }
            }
            override fun onConfigureFailed(session: CameraCaptureSession) {
                mainHandler.post { statusText.text = "Status: Camera Config Failed" }
            }
        }, cameraHandler)
    }

    private fun updateCameraSettings() {
        val session = captureSession ?: return
        val device = cameraDevice ?: return
        try {
            val builder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            builder.addTarget(imageReader!!.surface)

            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureMs * 1000000L)
            builder.set(CaptureRequest.SENSOR_SENSITIVITY, iso)

            session.setRepeatingRequest(builder.build(), null, cameraHandler)
        } catch (e: CameraAccessException) {
            Log.e("A2LiveStacker", "Failed to update settings", e)
        }
    }

    private fun closeCamera() {
        captureSession?.close()
        captureSession = null
        cameraDevice?.close()
        cameraDevice = null
        imageReader?.close()
        imageReader = null
        cameraThread?.quitSafely()
        cameraThread = null
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startStacking()
        }
    }
}
