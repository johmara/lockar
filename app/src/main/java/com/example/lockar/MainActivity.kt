package com.example.lockar

import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import org.opencv.android.*
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.sqrt


class MainActivity : CameraActivity(), View.OnTouchListener,
CvCameraViewListener2 {

    private val TAG = "OCVSample::Activity"
    private var mOpenCvCameraView: CameraBridgeViewBase? = null
    private val mSpectrum: Mat? = null
    private val SPECTRUM_SIZE: Size? = null
    private val CONTOUR_COLOR: Scalar? = null
    private var mRgba:Mat? = null
    private val mBlobColorRgba: Scalar? = null
    private val mBlobColorHsv: Scalar? = null
    private val mIsColorSelected = false

    private var imgHolder:Mat? = null
    lateinit var bwIMG:Mat
    lateinit var hsvIMG:Mat
    lateinit var lrrIMG:Mat
    lateinit var urrIMG:Mat
    lateinit var dsIMG:Mat
    lateinit var usIMG:Mat
    lateinit var cIMG:Mat
    lateinit var hovIMG:Mat

    var approxCurve: MatOfPoint2f? = null

    var threshold = 0

    private val mLoaderCallback: BaseLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                SUCCESS -> {
                    Log.i(TAG, "OpenCV loaded successfully")
                    initImgHolders()
                    mOpenCvCameraView!!.enableView()
                    //mOpenCvCameraView!!.setOnTouchListener(this@ColorBlobDetectionActivity)
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }

        private fun initImgHolders() {
            bwIMG = Mat()
            dsIMG = Mat()
            hsvIMG = Mat()
            lrrIMG = Mat()
            urrIMG = Mat()
            usIMG = Mat()
            cIMG = Mat()
            hovIMG = Mat()

            approxCurve = MatOfPoint2f()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        //requestWindowFeature(Window.FEATURE_NO_TITLE)
        //window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)


        mOpenCvCameraView = findViewById<View>(R.id.cameraViewer) as JavaCameraView?
        mOpenCvCameraView!!.visibility = SurfaceView.VISIBLE
        mOpenCvCameraView!!.setCvCameraViewListener(this)
        
    }

    override fun onPause() {
        super.onPause()
        mOpenCvCameraView?.disableView()
    }

    override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()){
            Log.d(TAG, "Internal OpenCV Library not found. Using OpenCV Manager for initialization")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback)
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!")
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    override fun getCameraViewList(): List<CameraBridgeViewBase?>? {
        return listOf(mOpenCvCameraView)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mOpenCvCameraView != null) mOpenCvCameraView?.disableView()
    }

    override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
        TODO("Not yet implemented")
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
    }

    override fun onCameraViewStopped() {
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        val gray = inputFrame!!.gray()
        val dst = inputFrame.rgba()

        Imgproc.pyrDown(
            gray, dsIMG, Size(
                (gray.cols() / 2).toDouble(),
                (gray.rows() / 2).toDouble()
            )
        )
        Imgproc.pyrUp(dsIMG, usIMG, gray.size())

        Imgproc.Canny(usIMG, bwIMG, 0.0, threshold.toDouble())

        Imgproc.dilate(bwIMG, bwIMG, Mat(), Point(), 1)

        val contours: List<MatOfPoint> = ArrayList()

        cIMG = bwIMG.clone()

        Imgproc.findContours(
            cIMG,
            contours,
            hovIMG,
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE
        )


        for (cnt in contours) {
            val curve = MatOfPoint2f(*cnt.toArray())
            Imgproc.approxPolyDP(curve, approxCurve, 0.02 * Imgproc.arcLength(curve, true), true)
            val numberVertices = approxCurve!!.total().toInt()
            val contourArea = Imgproc.contourArea(cnt)
            if (Math.abs(contourArea) < 100) {
                continue
            }

            //Rectangle detected
            if (numberVertices in 4..6) {
                val cos: MutableList<Double> = ArrayList()
                for (j in 2 until numberVertices + 1) {
                    cos.add(
                        angle(
                            approxCurve!!.toArray()[j % numberVertices],
                            approxCurve!!.toArray()[j - 2], approxCurve!!.toArray()[j - 1]
                        )
                    )
                }
                Collections.sort(cos)
                val mincos = cos[0]
                val maxcos = cos[cos.size - 1]
                if (numberVertices == 4 && mincos >= -0.1 && maxcos <= 0.3) {
                    setLabel(dst, "X", cnt)
                }
            }
        }

        return dst

        /* //get rgba image
        if (inputFrame != null) {
            imgHolder = inputFrame.rgba()
        };

        //to get grayscale image using below line
        //img = inputFrame.gray();

        return imgHolder!!;*/
    }

    private fun convertScalarHsv2Rgba(hsvColor: Scalar): Scalar? {
        val pointMatRgba = Mat()
        val pointMatHsv = Mat(1, 1, CvType.CV_8UC3, hsvColor)
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4)
        return Scalar(pointMatRgba[0, 0])
    }

    private fun angle(pt1: Point, pt2: Point, pt0: Point): Double {
        val dx1 = pt1.x - pt0.x
        val dy1 = pt1.y - pt0.y
        val dx2 = pt2.x - pt0.x
        val dy2 = pt2.y - pt0.y
        return (dx1 * dx2 + dy1 * dy2) / sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10)
    }
    private fun setLabel(im: Mat, label: String, contour: MatOfPoint) {
        val fontface: Int = Imgproc.FONT_HERSHEY_SIMPLEX
        val scale = 3.0 //0.4;
        val thickness = 3 //1;
        val baseline = IntArray(1)
        val text = Imgproc.getTextSize(label, fontface, scale, thickness, baseline)
        val r = Imgproc.boundingRect(contour)
        val pt = Point(r.x + (r.width - text.width) / 2, r.y + (r.height + text.height) / 2)
        Imgproc.putText(im, label, pt, fontface, scale, Scalar(255.0, 0.0, 0.0), thickness)
    }
}