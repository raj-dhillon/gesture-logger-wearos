package com.example.gesturelogger.presentation

import android.util.Log

import android.app.Activity
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import com.example.gesturelogger.R
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accel: Sensor? = null
    private var gyro: Sensor? = null
    private var isRecording = false
    private var outputStream: FileOutputStream? = null

    // Store latest readings
    private var lastAccel = FloatArray(3) { 0f }
    private var lastGyro = FloatArray(3) { 0f }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        val gestureLabel = findViewById<EditText>(R.id.gestureLabel)
        val startBtn = findViewById<Button>(R.id.startButton)
        val stopBtn = findViewById<Button>(R.id.stopButton)

        startBtn.setOnClickListener {
            val label = gestureLabel.text.toString().ifEmpty { "unknown" }
            updateButtonColors(startBtn, true)
            startRecording(label)
        }

        stopBtn.setOnClickListener {
            stopRecording()
            updateButtonColors(startBtn, false)
        }
    }

    private fun updateButtonColors(btn: Button, isRecording: Boolean) {
        if (isRecording) {
            btn.setBackgroundColor(getColor(R.color.recordingRed))
        } else {
            btn.setBackgroundColor(getColor(R.color.defaultButton))
        }
    }

    private fun startRecording(label: String) {
        if (isRecording) return
        isRecording = true

        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val fileName = "${label}_${sdf.format(Date())}.csv"
        val file = File(getExternalFilesDir(null), fileName)
        outputStream = FileOutputStream(file)

        // File path /storage/emulated/0/Android/data/com.example.gesturelogger/files
        Log.d("GestureLogger", "Writing CSV to: ${file.absolutePath}")


        // Write CSV header
        outputStream?.write("timestamp,ax,ay,az,gx,gy,gz,label\n".toByteArray())

        sensorManager.registerListener(this, accel, 10000)
        sensorManager.registerListener(this, gyro, 10000)
    }

    private fun stopRecording() {
        if (!isRecording) return
        isRecording = false

        sensorManager.unregisterListener(this)
        outputStream?.close()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isRecording || event == null) return

        val ts = System.currentTimeMillis()
        val label = findViewById<EditText>(R.id.gestureLabel).text.toString().ifEmpty { "unknown" }

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> lastAccel = event.values.clone()
            Sensor.TYPE_GYROSCOPE -> lastGyro = event.values.clone()
        }

        // Write a row combining latest accel + gyro
        val line = "$ts," +
                "${lastAccel[0]},${lastAccel[1]},${lastAccel[2]}," +
                "${lastGyro[0]},${lastGyro[1]},${lastGyro[2]}," +
                "$label\n"

        outputStream?.write(line.toByteArray())
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
