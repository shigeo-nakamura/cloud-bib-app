package com.cloudbib.client.ui.borrow

import BarcodeScanner
import BaseFragment
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.ToggleButton
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.cloudbib.client.R
import com.cloudbib.client.SharedToggleViewModel
import com.cloudbib.client.databinding.FragmentBorrowBinding

class BorrowFragment : BaseFragment(), BarcodeScanner.OnBarcodeScannedListener {

    private val TAG = "BorrowFragment"
    private var _binding: FragmentBorrowBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!
    private var userId: String? = null

    private lateinit var barcodeScanner: BarcodeScanner

    override fun onBarcodeScanned(barcode: String?, fromButton: String) {
        userId = barcode
        Log.d(TAG, "Scanned barcode: $userId, fromButton: $fromButton")

        val borrowButton = binding.buttonBorrow
        borrowButton.isEnabled = true
    }

    override fun onScanFailed() {
        Log.e(TAG, "Barcode scanning failed")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the barcode scanner with the fragment and listener
        barcodeScanner = BarcodeScanner(this, this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBorrowBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val sharedViewModel =
            ViewModelProvider(requireActivity())[SharedToggleViewModel::class.java]

        val toggleButton = binding.root.findViewById<ToggleButton>(R.id.connection_toggle)
        toggleButton.post {
            val toggleState = sharedViewModel.getToggleState().value ?: false
            toggleButton.isChecked = toggleState

            toggleButton.setOnCheckedChangeListener { _, isChecked ->
                // Do something when the toggle button is clicked
                if (isChecked) {
                    sharedViewModel.setToggleState(true)
                    // Toggle button is on
                } else {
                    sharedViewModel.setToggleState(false)
                    // Toggle button is off
                }
            }
        }

        val buttonSelectUser = binding.buttonSelectUser
        // Observe the state of the shared view model and update the state of the toggle button when it changes
        sharedViewModel.getToggleState().observe(viewLifecycleOwner) { state ->
            toggleButton.isChecked = state ?: false
            buttonSelectUser.isEnabled = state ?: false
        }

        buttonSelectUser.isEnabled = sharedViewModel.getToggleState().value ?: false

        buttonSelectUser.setOnClickListener {
            val borrowButton = binding.buttonBorrow
            borrowButton.isEnabled = false

            barcodeScanner.start("buttonSelectUser")
        }

        val buttonBorrow = binding.buttonBorrow
        buttonBorrow.setOnClickListener {
            barcodeScanner.start("buttonBorrow")
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 123
    }
}
