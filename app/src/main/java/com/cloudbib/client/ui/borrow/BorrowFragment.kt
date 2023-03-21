package com.cloudbib.client.ui.borrow

import androidx.fragment.app.Fragment
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ToggleButton
import androidx.lifecycle.ViewModelProvider
import com.cloudbib.client.*
import com.cloudbib.client.databinding.FragmentBorrowBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.cloudbib.client.BarcodeScannerViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class BorrowFragmentViewModel : ViewModel() {
    private val _unameViewText = MutableLiveData<String>().apply { value = "" }
    val unameViewText: LiveData<String> = _unameViewText

    private val _numBookViewText = MutableLiveData<String>().apply { value = "" }
    val numBookViewText: LiveData<String> = _numBookViewText

    private val _borrowStatusViewText = MutableLiveData<String>().apply { value = "" }
    val borrowStatusViewText: LiveData<String> = _borrowStatusViewText

    private val _borrowTitleViewText = MutableLiveData<String>().apply { value = "" }
    val borrowTitleViewText: LiveData<String> = _borrowTitleViewText

    // Add this variable to keep track of the state of the buttonBorrow
    private val _isButtonBorrowEnabled = MutableLiveData<Boolean>().apply { value = false }
    val isButtonBorrowEnabled: LiveData<Boolean> = _isButtonBorrowEnabled

    // Update the value of isButtonBorrowEnabled when the button is enabled or disabled
    fun setButtonBorrowEnabled(isEnabled: Boolean) {
        _isButtonBorrowEnabled.value = isEnabled
    }

    fun setUnameViewText(text: String) {
        _unameViewText.value = text
    }

    fun setNumBookViewText(text: String) {
        _numBookViewText.value = text
    }

    fun setBorrowStatusViewText(text: String) {
        _borrowStatusViewText.value = text
    }

    fun setBorrowTitleViewText(text: String) {
        _borrowTitleViewText.value = text
    }

    fun clearTextFields() {
        setUnameViewText("")
        setNumBookViewText("")
        setBorrowStatusViewText("")
        setBorrowTitleViewText("")
    }
}

class BorrowFragment : Fragment(), BarcodeScanner.OnBarcodeScannedListener {

    private val tag = "BorrowFragment"
    private var _binding: FragmentBorrowBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!
    private var userId: String? = null

    private lateinit var barcodeScanner: BarcodeScanner
    private lateinit var borrowFragmentViewModel: BorrowFragmentViewModel
    private lateinit var barcodeScannerViewModel: BarcodeScannerViewModel

    private fun clearUserField() {
        borrowFragmentViewModel.setUnameViewText("")
        borrowFragmentViewModel.setNumBookViewText("")
    }

    private fun clearBorrowState() {
        borrowFragmentViewModel.setBorrowStatusViewText("")
        borrowFragmentViewModel.setBorrowTitleViewText("")
    }

    private fun updateUserField(res: HttpResponse) {
        borrowFragmentViewModel.setUnameViewText(res.user?.name.toString())
        borrowFragmentViewModel.setNumBookViewText(res.num_borrowed_book.toString())
    }

    private fun updateBorrowField(res: HttpResponse) {
        borrowFragmentViewModel.setNumBookViewText(res.num_borrowed_book.toString())
        borrowFragmentViewModel.setBorrowTitleViewText(res.borrowed_book?.title.toString())
        borrowFragmentViewModel.setBorrowStatusViewText("貸出処理が完了しました")
    }

    override fun onBarcodeScanned(barcode: String?, fromButton: String) {
        userId = barcode
        Log.d(tag, "Scanned barcode: $userId, fromButton: $fromButton")

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
                val res: HttpResponse = if (fromButton == "buttonSelectUser") {
                    withContext(Dispatchers.IO) {
                        httpUtility.selectUser(barcode)
                    }
                } else {
                    withContext(Dispatchers.IO) {
                        httpUtility.borrowBook(barcode)
                    }
                }
                Log.d(tag, res.toString())
                val statusView = requireView().findViewById<TextView>(R.id.borrowStatusView)

                if (res.success) {
                    if (fromButton == "buttonSelectUser") {
                        updateUserField(res)
                        Log.d(tag, "enabled the buttonBorrow")
                        // Set the value of isButtonBorrowEnabled to true and enable the buttonBorrow
                        borrowFragmentViewModel.setButtonBorrowEnabled(true)
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
                Log.e(tag, "Exception while returning book: ${e.message}")
                sharedViewModel.setToggleState(false)
            }
        }
    }

    override fun onScanFailed() {
        Log.e(tag, "Barcode scanning failed")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the BarcodeScannerViewModel and the BarcodeScanner with the ViewModel instance
        barcodeScannerViewModel = ViewModelProvider(this)[BarcodeScannerViewModel::class.java]
        barcodeScanner = BarcodeScanner(this, barcodeScannerViewModel, this)
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

        borrowFragmentViewModel = ViewModelProvider(this)[BorrowFragmentViewModel::class.java]

        borrowFragmentViewModel.unameViewText.observe(viewLifecycleOwner) { text ->
            binding.unameView.text = text
        }

        borrowFragmentViewModel.numBookViewText.observe(viewLifecycleOwner) { text ->
            binding.numBookView.text = text
        }

        borrowFragmentViewModel.borrowStatusViewText.observe(viewLifecycleOwner) { text ->
            binding.borrowStatusView.text = text
        }

        borrowFragmentViewModel.borrowTitleViewText.observe(viewLifecycleOwner) { text ->
            binding.borrowTitleView.text = text
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

        // Observe the state of isButtonBorrowEnabled and update the buttonBorrow accordingly
        borrowFragmentViewModel.isButtonBorrowEnabled.observe(viewLifecycleOwner) { isEnabled ->
            binding.buttonBorrow.isEnabled = isEnabled
        }

        buttonSelectUser.setOnClickListener {
            clearUserField()
            clearBorrowState()
            binding.buttonBorrow.isEnabled = false
            barcodeScannerViewModel.fromButton = "buttonSelectUser"
            barcodeScanner.start(barcodeScannerViewModel.fromButton!!)
        }

        buttonBorrow.setOnClickListener {
            clearBorrowState()
            barcodeScannerViewModel.fromButton = "buttonBorrow"
            barcodeScanner.start(barcodeScannerViewModel.fromButton!!)

        }

        return root
    }

    override fun onDestroyView() {
        Log.d(tag, "onDestroyView")
        super.onDestroyView()
        _binding = null
    }
}
