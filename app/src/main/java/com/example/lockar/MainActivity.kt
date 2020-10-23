package com.example.lockar

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import com.example.lockar.databinding.ActivityMainBinding
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.opencv.android.OpenCVLoader


class MainActivity : AppCompatActivity() {
    private val TAG = "OCVSample::Activity"

    private lateinit var drawerLayout: DrawerLayout


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("UNUSED_VARIABLE")
        val binding = DataBindingUtil
            .setContentView<ActivityMainBinding>(this, R.layout.activity_main)
        val navController = this.findNavController(R.id.myNavHostFragment)

        drawerLayout = binding.drawerLayout

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA)
        }

    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = this.findNavController(R.id.myNavHostFragment)
        return NavigationUI.navigateUp(navController, drawerLayout)
    }

    fun onStartupClicked(view: View){
        view.findNavController().navigate(StartupFragmentDirections.actionStartupFragmentToDoorDetection())
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