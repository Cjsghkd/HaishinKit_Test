package com.example.haishinkit_test

import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.haishinkit.event.Event
import com.haishinkit.event.EventUtils
import com.haishinkit.event.IEventListener
import com.haishinkit.graphics.VideoGravity
import com.haishinkit.media.AudioRecordSource
import com.haishinkit.media.Camera2Source
import com.haishinkit.rtmp.RtmpConnection
import com.haishinkit.rtmp.RtmpStream
import com.haishinkit.view.HkSurfaceView

class TestActivity : AppCompatActivity(), IEventListener {

    private lateinit var connection: RtmpConnection
    private lateinit var rtmpStream: RtmpStream
    private lateinit var hkView: HkSurfaceView

    companion object {
        private const val TAG = "RTMP"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        hkView = findViewById(R.id.hk_view)

        hkView.post {
            initializeRtmpConnection()
            initializeRtmpStream()
            connectToRtmpServer()
        }
    }

    private fun initializeRtmpConnection() {
        connection = RtmpConnection()
        connection.addEventListener(Event.RTMP_STATUS, this)
        connection.addEventListener(Event.IO_ERROR, this)
        Log.d("RTMP", "initializeRtmpConnection")
    }

    private fun initializeRtmpStream() {
        rtmpStream = RtmpStream(applicationContext, connection)
        rtmpStream.audioSetting.apply {
            bitRate = 44100
            sampleRate = 48000
        }
        rtmpStream.videoSetting.apply {
            width = 1280
            height = 720
            frameRate = 24 // Default 30
            bitRate = 1000
        }
        Log.d(TAG, "Audio settings applied: bitRate=${rtmpStream.audioSetting.bitRate}, sampleRate=${rtmpStream.audioSetting.sampleRate}, channelCount=${rtmpStream.audioSetting.channelCount}")
        Log.d(TAG, "Video settings applied: bitRate=${rtmpStream.videoSetting.bitRate}, frameRate=${rtmpStream.videoSetting.frameRate}, width=${rtmpStream.videoSetting.width}, height=${rtmpStream.videoSetting.height}")
        Log.d(TAG, "receive applied1: receiveAudio=${rtmpStream.receiveAudio}, receiveVideo=${rtmpStream.receiveVideo}")
    }

    private fun connectToRtmpServer() {
        try {
            if (hkView.isAttachedToWindow) {
                Log.d(TAG, "hkView is attached to window and ready")
            } else {
                Log.e(TAG, "hkView is not attached to window!")
            }
            hkView.post {
                Log.d(TAG, "hkview.post >> ${hkView.isAttachedToWindow}")
                hkView.attachStream(rtmpStream)
                connection.connect("RTMP")
            }

            Log.d(TAG, "Connection attempt made")
        } catch (e: Exception) {
            Log.e(TAG, "Connection error: ${e.message}", e)
        }
    }

    override fun handleEvent(event: Event) {
        Log.d(TAG, "handleEvent => ${event.type}")
        when (event.type) {
            Event.RTMP_STATUS -> {
                val data = EventUtils.toMap(event)
                when (data["code"]) {
                    RtmpConnection.Code.CONNECT_SUCCESS.rawValue -> {
                        Log.d(TAG, "Connection success: $data")

                        try {
                            rtmpStream.play("NAME")
                            Log.d(TAG, "stream.play called successfully")
                            Log.d(TAG, "receive applied2: receiveAudio=${rtmpStream.receiveAudio}, receiveVideo=${rtmpStream.receiveVideo}")
                            if (connection.isConnected) Log.d(TAG, "isConnected")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error during play: ${e.message}", e)
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
            Event.IO_ERROR -> {
                Log.d(TAG, "IO error => ${event.data}")
            }
            else -> {
                Log.d(TAG, "else error => ${event.type}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        rtmpStream.close()
        connection.close()
        Log.d(TAG, "Connection and stream closed")
    }
}