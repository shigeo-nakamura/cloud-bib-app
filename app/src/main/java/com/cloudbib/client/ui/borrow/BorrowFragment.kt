package com.cloudbib.client.ui.borrow

import BarcodeScanner
import HttpResponse
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ToggleButton
import androidx.lifecycle.ViewModelProvider
import com.cloudbib.client.R
import com.cloudbib.client.SharedToggleViewModel
import com.cloudbib.client.databinding.FragmentBorrowBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BorrowFragment : Fragment(), BarcodeScanner.OnBarcodeScannedListener {

    private val TAG = "BorrowFragment"
    private var _binding: FragmentBorrowBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!
    private var userId: String? = null

    private lateinit var barcodeScanner: BarcodeScanner

    private fun clearUserField() {
        requireView().findViewById<TextView>(R.id.unameView).text = ""
        requireView().findViewById<TextView>(R.id.numBookView).text =" "
    }

    private fun clearBorrowState() {
        requireView().findViewById<TextView>(R.id.borrowStatusView).text = ""
        requireView().findViewById<TextView>(R.id.borrowTitleView).text = " "
    }

    private fun updateUserField(res: HttpResponse) {
        requireView().findViewById<TextView>(R.id.unameView).text = res.user?.name.toString()
        requireView().findViewById<TextView>(R.id.numBookView).text = res.num_borrowed_book.toString()
    }

    private fun updateBorrowField(res: HttpResponse) {
        requireView().findViewById<TextView>(R.id.numBookView).text = res.num_borrowed_book.toString()
        requireView().findViewById<TextView>(R.id.borrowTitleView).text = res.borrowed_book?.title.toString()
        requireView().findViewById<TextView>(R.id.borrowStatusView).text = "貸出処理が完了しました".toString()
    }

    override fun onBarcodeScanned(barcode: String?, fromButton: String) {
        userId = barcode
        Log.d(TAG, "Scanned barcode: $userId, fromButton: $fromButton")

        Log.d(TAG, "find : $barcode")
        val sharedViewModel =
            ViewModelProvider(requireActivity()).get(SharedToggleViewModel::class.java)
        val httpUtility = sharedViewModel.getHttpUtility()

        if (barcode == null) {
            Log.d(TAG, "symbol not found")
            return
        }

        // Launch a coroutine to execute return_book() on a background thread
        CoroutineScope(Dispatchers.Main).launch {
            try {
                var res: HttpResponse;
                if (fromButton == "buttonSelectUser") {
                    res = withContext(Dispatchers.IO) {
                        httpUtility.selectUser(barcode)
                    }
                }
                else {
                    res = withContext(Dispatchers.IO) {
                        httpUtility.borrowBook(barcode)
                    }
                }
                Log.d(TAG, res.toString())
                var statusView = requireView().findViewById<TextView>(R.id.borrowStatusView)

                if (res.success) {
                    if (fromButton == "buttonSelectUser") {
                        updateUserField(res)
                        binding.buttonBorrow.isEnabled = true
                    } else {
                        updateBorrowField(res)
                    }
                } else {
                    when (res.errorCode) {
                        106 -> {
                            statusView.text = "該当する利用者が見つかりません"
                        }
                        107 -> {
                            statusView.text = "該当する図書が見つかりません"
                        }
                        110 -> {
                            statusView.text = "この本は返却されていません"
                        }
                        else -> {
                            statusView.text = "データが見つかりません"
                        }
                    }
                }
            } catch (e: Exception) {
                // Handle the exception
                Log.e(TAG, "Exception while returning book: ${e.message}")
                sharedViewModel.setToggleState(false)
            }
        }
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
        val buttonBorrow = binding.buttonBorrow
        // Observe the state of the shared view model and update the state of the toggle button when it changes
        sharedViewModel.getToggleState().observe(viewLifecycleOwner) { state ->
            toggleButton.isChecked = state ?: false
            buttonSelectUser.isEnabled = state ?: false
            if (!state) {
                clearUserField()
                clearBorrowState()
                buttonBorrow.isEnabled = false
            }
        }

        binding.buttonBorrow.isEnabled = false

        buttonSelectUser.setOnClickListener {
            clearUserField()
            clearBorrowState()
            binding.buttonBorrow.isEnabled = false
            barcodeScanner.start("buttonSelectUser")
        }

        buttonBorrow.setOnClickListener {
            clearBorrowState()
            barcodeScanner.start("buttonBorrow")
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
