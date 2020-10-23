package com.example.lockar


import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.View.*
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import org.opencv.android.*
import org.opencv.core.CvType
import org.opencv.core.Mat

class DoorDetection : CameraActivity() , OnTouchListener,
    CameraBridgeViewBase.CvCameraViewListener2 {

    private val TAG = "OCV_DOOR::Activity"

    private var mRgba: Mat? = null
    private var mOpenCvCameraView: CameraBridgeViewBase? = null

    private val mLoaderCallback: BaseLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                SUCCESS -> {
                    Log.i(TAG,
                        "OpenCV loaded successfully")
                    mOpenCvCameraView!!.enableView()
                    mOpenCvCameraView!!.setOnTouchListener(this@DoorDetection) // if we want touch
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_door_detection)

        mOpenCvCameraView = findViewById(R.id.camera_viewer)
        mOpenCvCameraView!!.visibility = VISIBLE
        mOpenCvCameraView!!.setCvCameraViewListener(this)
    }

    override fun getCameraViewList(): List<CameraBridgeViewBase?>?{
        return listOf(mOpenCvCameraView)
    }

    override fun onPause() {
        super.onPause()
        if (mOpenCvCameraView != null) mOpenCvCameraView!!.disableView()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mOpenCvCameraView != null) mOpenCvCameraView!!.disableView()
    }


    override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()){
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback)
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }


    override fun onCameraViewStarted(width: Int, height: Int) {
        mRgba = Mat(height, width, CvType.CV_8UC4)
    }

    override fun onCameraViewStopped() { mRgba!!.release() }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        mRgba = inputFrame!!.rgba()

        Log.i(TAG, mRgba.toString())

        return mRgba!!
    }

    override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
        var toast = Toast.makeText(this, "camera pressed", LENGTH_SHORT)
        toast.show()
        return false
    }

}
