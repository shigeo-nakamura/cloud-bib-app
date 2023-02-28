package com.cloudbib.client.ui.`return`

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.cloudbib.client.R
import com.cloudbib.client.SharedToggleViewModel
import com.cloudbib.client.databinding.FragmentReturnBinding
import com.google.zxing.integration.android.IntentIntegrator
import android.Manifest
import androidx.activity.result.ActivityResultLauncher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ReturnFragment : Fragment() {

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1
    }

    private val TAG = "ReturnFragment"
    private var _binding: FragmentReturnBinding? = null

    private var scanResult: ((Int, Intent?) -> Unit)? = null


    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!
    private lateinit var toggleButton: ToggleButton

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Camera permission was granted, launch camera
            launchCamera()
        } else {
            // Camera permission was denied
            Toast.makeText(requireContext(), "Camera permission was denied", Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val returnViewModel = ViewModelProvider(this).get(ReturnViewModel::class.java)

        _binding = FragmentReturnBinding.inflate(inflater, container, false)
        val root: View = binding.root

        scanResult = { resultCode, data ->
            Log.d(TAG, "scanResult lambda called")
            val intentResult = IntentIntegrator.parseActivityResult(resultCode, data)
            if (intentResult != null && intentResult.contents != null) {
                // Barcode was successfully scanned
                val symbol = intentResult.contents
                Log.d(TAG, "find : $symbol")
                val sharedViewModel =
                    ViewModelProvider(requireActivity()).get(SharedToggleViewModel::class.java)
                val httpUtility = sharedViewModel.getHttpUtility()

                // Launch a coroutine to execute return_book() on a background thread
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val res = withContext(Dispatchers.IO) {
                            httpUtility.return_book(symbol)
                        }
                        Log.d(TAG, res.toString())
                        var statusView = requireView().findViewById<TextView>(R.id.statusView)

                        if (res.success) {
                            statusView.text = "返却しました"
                            requireView().findViewById<TextView>(R.id.returnerView).text = res.user?.name
                            requireView().findViewById<TextView>(R.id.titleView).text =
                                res.returned_book_title
                        } else {
                            when (res.errorCode) {
                                107 -> {
                                    statusView.text = "該当図書が見つかりません"
                                }
                                111 -> {
                                    statusView.text = "この本は貸出されていません"
                                    requireView().findViewById<TextView>(R.id.returnerView).text = ""
                                    requireView().findViewById<TextView>(R.id.titleView).text = ""
                                }
                                else -> {
                                    statusView.text = "データが見つかりません"
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Handle the exception
                        Log.e(TAG, "Exception while returning book: ${e.message}")
                    }
                }
            } else {
                Log.d(TAG, "symbol not found")
            }
        }



        val textView: TextView = binding.textReturn
        returnViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        toggleButton = binding.root.findViewById(R.id.connection_toggle)

        val sharedViewModel =
            ViewModelProvider(requireActivity()).get(SharedToggleViewModel::class.java)

        toggleButton.post {
            val toggleState = sharedViewModel.getToggleState().value ?: false
            toggleButton.isChecked = toggleState
            toggleButton.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    sharedViewModel.setToggleState(true)
                } else {
                    sharedViewModel.setToggleState(false)
                }
            }
        }

        sharedViewModel.getToggleState().observe(viewLifecycleOwner) { state ->
            toggleButton.isChecked = state ?: false
        }

        val buttonReturn = binding.buttonReturn
        buttonReturn.isEnabled = sharedViewModel.getToggleState().value ?: false
        sharedViewModel.getToggleState().observe(viewLifecycleOwner) { state ->
            buttonReturn.isEnabled = state ?: false
        }

        buttonReturn.setOnClickListener {
            startBarcodeScanning()
        }

        return root
    }



    // Call this function to start the barcode scanning
    private fun startBarcodeScanning() {
        Log.d(TAG, "startBarcodeScanning")

        requireView().findViewById<TextView>(R.id.statusView).text = ""
        requireView().findViewById<TextView>(R.id.returnerView).text = ""
        requireView().findViewById<TextView>(R.id.titleView).text = ""

        // Get the current screen orientation
        val currentOrientation = resources.configuration.orientation

        // Set the screen orientation to portrait
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // Check if camera permission has been granted
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Launch camera
            launchCamera()
        } else {
            // Request camera permission
            requestCameraPermission()
        }

        // Reset the screen orientation to its original state
        activity?.requestedOrientation = currentOrientation
    }

    private fun launchCamera() {
        Log.d(TAG, "launchCamera")
        val integrator = IntentIntegrator.forSupportFragment(this)
        integrator.setOrientationLocked(false)
        integrator.setPrompt("Scan a barcode")
        integrator.setBeepEnabled(false)
        integrator.initiateScan()
    }

    private fun requestCameraPermission() {
        Log.d(TAG, "requestCameraPermission")
        requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Call the scanResult callback function if it has been set
        scanResult?.invoke(resultCode, data)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewCreated")
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView")
        super.onDestroyView()
        _binding = null
    }
}
