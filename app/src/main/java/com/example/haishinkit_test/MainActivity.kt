package com.example.haishinkit_test

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.media.AudioFormat
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.haishinkit.event.Event
import com.haishinkit.event.IEventListener
import com.haishinkit.graphics.VideoGravity
import com.haishinkit.media.AudioRecordSource
import com.haishinkit.media.Camera2Source
import com.haishinkit.media.MultiCamera2Source
import com.haishinkit.rtmp.RtmpConnection
import com.haishinkit.rtmp.RtmpStream
import com.haishinkit.screen.Screen
import com.haishinkit.view.HkSurfaceView

class MainActivity : AppCompatActivity(), IEventListener {

    private lateinit var connection: RtmpConnection
    private lateinit var stream: RtmpStream
    private lateinit var cameraView: HkSurfaceView

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 1001
        private const val TAG = "RTMP"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (checkAndRequestPermissions()) {
            setupStreaming()
        }
    }

    fun checkAndRequestPermissions(): Boolean {
        val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest, PERMISSIONS_REQUEST_CODE)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.d(TAG, "All permissions granted")
                setupStreaming()
            } else {
                Log.e(TAG, "Permissions not granted")
                // 권한이 거부됨을 사용자에게 알림
            }
        }
    }

    private fun setupStreaming() {
        Log.d(TAG, "Setting up streaming")

        connection = RtmpConnection()
        stream = RtmpStream(applicationContext, connection)

        // 이벤트 리스너 등록
        connection.addEventListener(Event.RTMP_STATUS, this)

        // 오디오 소스 연결
        stream.attachAudio(AudioRecordSource(applicationContext))
        Log.d(TAG, "Audio source attached")

        // 스트림 설정
        stream.videoSetting.width = 1280
        stream.videoSetting.height = 720
        stream.videoSetting.frameRate = 24
        stream.videoSetting.bitRate = 1000

        stream.audioSetting.bitRate = 44100
        stream.audioSetting.sampleRate = 48000

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            stream.attachVideo(Camera2Source(applicationContext))
            Log.d(TAG, "attachVideo >> ${stream.videoSource is Camera2Source}, :: ${stream.videoSource}")
            (stream.videoSource as? Camera2Source)?.apply {
                open(CameraCharacteristics.LENS_FACING_FRONT)
            }
            Log.d(TAG, "Video settings: width=${stream.videoSetting.width}, height=${stream.videoSetting.height}, frameRate=${stream.videoSetting.frameRate}, bitRate=${stream.videoSetting.bitRate}")
        } else {
            stream.attachVideo(Camera2Source(applicationContext))

            (stream.videoSource as? Camera2Source)?.apply {
                open(CameraCharacteristics.LENS_FACING_FRONT)
            }
        }

        // 카메라 뷰 설정
        cameraView = findViewById(R.id.camera_view)
        cameraView.attachStream(stream)
        Log.d(TAG, "Stream attached to view")

        // RTMP 서버에 연결
        connection.connect("RTMP")
        Log.d(TAG, "Connecting to RTMP server")

    }

    override fun handleEvent(event: Event) {
        when (event.type) {
            Event.RTMP_STATUS -> {
                val data = event.data as Map<*, *>
                when (data["code"]) {
                    RtmpConnection.Code.CONNECT_SUCCESS.rawValue -> {
                        Log.d(TAG, "Connection success: $data")
                        runOnUiThread {
                            stream.publish("Name")
                            Log.d(TAG, "Stream publishing started")
                        }
                    }
                    RtmpConnection.Code.CONNECT_FAILED.rawValue -> {
                        Log.e(TAG, "Connection failed: ${data["description"]}")
                    }
                    else -> {
                        Log.d(TAG, "RTMP status: $data")
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        connection.close()
        stream.close()
        Log.d(TAG, "Connection and stream closed")
    }
}