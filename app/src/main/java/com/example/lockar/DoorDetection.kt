package com.example.lockar


import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Path
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.View.OnTouchListener
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.lockar.views.DrawView
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.opencv.android.*
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.collections.ArrayList


class DoorDetection : CameraActivity() , OnTouchListener,
    CameraBridgeViewBase.CvCameraViewListener2 {

    private val TAG = "OCV_DOOR::Activity"

    private var mRgba: Mat? = null
    private var mIntermediateMat: Mat? = null
    private var disposable: Disposable? = null
    private val subject = PublishSubject.create<CameraData>()
    private lateinit var cameraData: CameraData
    private val toast: Toast? = null
    var yes = true
    var start = true

    private var drawView: DrawView? = null

    private var grey: Mat? = null



    /**
     * approximated polygonal curve with specified precision
     */
    private lateinit var approxCurve: MatOfPoint2f

    private var mOpenCvCameraView: CameraBridgeViewBase? = null

    private val mLoaderCallback: BaseLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                SUCCESS -> {
                    Log.i(TAG,
                        "OpenCV loaded successfully")

                    grey = Mat()
                    mIntermediateMat = Mat()

                    approxCurve = MatOfPoint2f()


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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA)
        }

        mOpenCvCameraView = findViewById(R.id.camera_viewer)
        mOpenCvCameraView!!.visibility = SurfaceView.VISIBLE
        mOpenCvCameraView!!.setCvCameraViewListener(this)
    }

    private fun init() {

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
        this.disposable?.dispose()
        if (mOpenCvCameraView != null) mOpenCvCameraView!!.disableView()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                runOnUiThread { init() }
            } else {
                finish()
            }
        }
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

    override fun onCameraViewStopped() {
        mRgba!!.release()

        // Explicitly deallocate Mats
        mIntermediateMat?.release()

        mIntermediateMat = null
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        mRgba = inputFrame!!.rgba()

        val mat: Mat = mRgba!!

        val ratio = (mOpenCvCameraView!!.height.toFloat().div(mRgba!!.height()))
        disposable =
        detectRect(mat, ratio).compose(mainAsync())
            .subscribe {path ->
                if (drawView != null){
                    drawView!!.setPath(path)
                    drawView!!.invalidate()
                }
            }

        return mRgba!!
    }

    override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
        var imgLock = findViewById<ImageView>(R.id.img_lock)

        if (start) {
            if (yes) {
                imgLock.setImageResource(R.drawable.ic_locked_lock)

                yes = false
            } else {
                imgLock.setImageResource(R.drawable.ic_open_lock)

                yes = true
                start = false
            }
            imgLock.visibility = View.VISIBLE
        }else{
            imgLock.visibility = View.GONE
            start = true
        }

        return false
    }

    private fun detectRect(mat: Mat, ratio: Float): Observable<Path> {
        return Observable.just(mat)
            .concatMap { resizeMat ->
                OpenCVHelper.getMonochromeMat(resizeMat)
                    .flatMap { monoChromeMat -> OpenCVHelper.getContoursMat(monoChromeMat, resizeMat) }
                    .flatMap { points -> Observable.just(points).flatMapIterable { e -> e }.map { e -> Point(e.x * ratio, e.y * ratio) }.toList().toObservable() }
                    .flatMap { points -> OpenCVHelper.getPath(points) }
            }
    }

    /**
     * Helper function to find a cosine of angle between vectors
     * from pt0->pt1 and pt0->pt2
     */
    private fun angle(pt1: Point, pt2: Point, pt0: Point): Double {
        val dx1 = pt1.x - pt0.x
        val dy1 = pt1.y - pt0.y
        val dx2 = pt2.x - pt0.x
        val dy2 = pt2.y - pt0.y
        return ((dx1 * dx2 + dy1 * dy2)
                / Math.sqrt(
            (dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10
        ))
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val REQUEST_CAMERA = 1
        private const val SIZE = 400

        init {
            if (!OpenCVLoader.initDebug()) {
                Log.v(TAG, "init OpenCV")
            }
        }

        private fun <T> mainAsync(): ObservableTransformer<T, T> {
            return ObservableTransformer { obs ->
                obs.subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread())
            }
        }
    }
}