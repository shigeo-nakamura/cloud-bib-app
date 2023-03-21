package com.cloudbib.client.ui.`return`

import com.cloudbib.client.BarcodeScanner
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
import com.cloudbib.client.databinding.FragmentReturnBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReturnFragment : Fragment(), BarcodeScanner.OnBarcodeScannedListener {
    private val tag = "ReturnFragment"
    private var _binding: FragmentReturnBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var barcodeScanner: BarcodeScanner

    private fun clearReturnField() {
        requireView().findViewById<TextView>(R.id.statusView).text = ""
        requireView().findViewById<TextView>(R.id.titleView).text = ""
        requireView().findViewById<TextView>(R.id.returnerView).text = ""
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
        val returnViewModel = ViewModelProvider(this)[ReturnViewModel::class.java]

        _binding = FragmentReturnBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textReturn
        returnViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        val toggleButton = binding.root.findViewById<ToggleButton>(R.id.connection_toggle)

        val sharedViewModel =
            ViewModelProvider(requireActivity())[SharedToggleViewModel::class.java]

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
            if (!state) {
                clearReturnField()
            }
        }

        buttonReturn.setOnClickListener {
            startBarcodeScanning()
        }

        return root
    }

    // Call this function to start the barcode scanning
    private fun startBarcodeScanning() {
        Log.d(tag, "startBarcodeScanning")

        clearReturnField()

        // Start the barcode scanning process using the com.cloudbib.client.BarcodeScanner
        barcodeScanner.start("buttonReturn")
    }

    override fun onBarcodeScanned(barcode: String?, fromButton: String) {
        Log.d(tag, "onBarcodeScanned")

        Log.d(tag, "find : $barcode")
        val sharedViewModel =
            ViewModelProvider(requireActivity())[SharedToggleViewModel::class.java]
        val httpUtility = sharedViewModel.getHttpUtility()

        if (barcode == null) {
            Log.d(tag, "symbol not found")
            return
        }

        // Launch a coroutine to execute return_book() on a background thread
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val res = withContext(Dispatchers.IO) {
                    httpUtility.returnBook(barcode)
                }
                Log.d(tag, res.toString())
                val statusView = requireView().findViewById<TextView>(R.id.statusView)

                if (res.success) {
                    statusView.text = "返却しました"
                    requireView().findViewById<TextView>(R.id.returnerView).text =
                        res.user?.name
                    requireView().findViewById<TextView>(R.id.titleView).text =
                        res.returned_book_title
                } else {
                    when (res.errorCode) {
                        107 -> {
                            statusView.text = "該当図書が見つかりません"
                        }
                        111 -> {
                            statusView.text = "この本は貸出されていません"
                            requireView().findViewById<TextView>(R.id.returnerView).text =
                                ""
                            requireView().findViewById<TextView>(R.id.titleView).text = ""
                        }
                        else -> {
                            statusView.text = "データが見つかりません"
                        }
                    }
                }
            } catch (e: Exception) {
                // Handle the exception
                Log.e(tag, "Exception while returning book: ${e.message}")
                sharedViewModel.setToggleState(false)
            }
        }
    }

    override fun onScanFailed() {
        Log.d(tag, "symbol not found")
    }

    override fun onDestroyView() {
        Log.d(tag, "onDestroyView")
        super.onDestroyView()
        _binding = null
    }
}
