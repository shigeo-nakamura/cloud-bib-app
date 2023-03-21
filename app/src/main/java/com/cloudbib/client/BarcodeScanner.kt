package com.cloudbib.client

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.google.zxing.integration.android.IntentIntegrator

class BarcodeScanner(
    private val fragment: Fragment,
    private val listener: OnBarcodeScannedListener
) {
    private val tag = "com.cloudbib.client.BarcodeScanner"
    private var fromButton = ""

    interface OnBarcodeScannedListener {
        fun onBarcodeScanned(barcode: String?, fromButton: String)
        fun onScanFailed()
    }

    private var barcodeScanLauncher: ActivityResultLauncher<Intent>
    private var cameraPermissionLauncher: ActivityResultLauncher<String>
    private var originalOrientation = 0

    init {
        cameraPermissionLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Camera permission was granted, launch barcode scanner
                launchBarcodeScanner()
            } else {
                // Camera permission denied
                listener.onScanFailed()
            }
        }

        barcodeScanLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val scanResult =
                IntentIntegrator.parseActivityResult(result.resultCode, result.data)
            if (scanResult != null && scanResult.contents != null) {
                listener.onBarcodeScanned(scanResult.contents, fromButton)
            } else {
                listener.onScanFailed()
            }
            // Restore the original orientation after the scan is complete
            //fragment.requireActivity().requestedOrientation = originalOrientation
        }
    }

    fun start(fromButton: String) {
        Log.d(tag, "start")

        this@BarcodeScanner.fromButton = fromButton

        // Check if camera permission has been granted
        if (ContextCompat.checkSelfPermission(
                fragment.requireContext(),
                Manifest.permission.CAMERA
            ) == androidx.core.content.PermissionChecker.PERMISSION_GRANTED
        ) {
            // Launch camera to scan barcode
            launchBarcodeScanner()
        } else {
            // Request camera permission
            requestCameraPermission()
        }
    }

    private fun launchBarcodeScanner() {
        Log.d(tag, "launchBarcodeScanner")

        originalOrientation = fragment.requireActivity().requestedOrientation
        val integrator = IntentIntegrator.forSupportFragment(fragment)
        fragment.requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        //integrator.setOrientationLocked(true)
        integrator.setPrompt("Scan a barcode")
        integrator.setBeepEnabled(false)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
        barcodeScanLauncher.launch(integrator.createScanIntent())
    }

    private fun requestCameraPermission() {
        Log.d(tag, "requestCameraPermission")

        if (fragment.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            Log.e(tag, "Fragment is not yet created")
        }
    }
}
