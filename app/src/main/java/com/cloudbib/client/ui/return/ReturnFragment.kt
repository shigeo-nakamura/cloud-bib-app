package com.cloudbib.client.ui.`return`

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ToggleButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.cloudbib.client.R
import com.cloudbib.client.SharedToggleViewModel
import com.cloudbib.client.databinding.FragmentReturnBinding


class ReturnFragment : Fragment() {

    private val TAG = "ReturnFragment"
    private var _binding: FragmentReturnBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var toggleButton: ToggleButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val returnViewModel =
            ViewModelProvider(this).get(ReturnViewModel::class.java)

        _binding = FragmentReturnBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textReturn
        returnViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        toggleButton = binding.root.findViewById(R.id.connection_toggle)

        val sharedViewModel = ViewModelProvider(requireActivity()).get(SharedToggleViewModel::class.java)

        toggleButton.post {
            val toggleState = sharedViewModel.getToggleState().value ?: false
            Log.d(TAG, "initial state: $toggleState")
            toggleButton.isChecked = toggleState
            toggleButton.setOnCheckedChangeListener { buttonView, isChecked ->
                // Do something when the toggle button is clicked

                if (isChecked) {
                    Log.d(TAG, "checked")
                    sharedViewModel.setToggleState(true)
                    // Toggle button is on
                } else {
                    Log.d(TAG, "unchecked")
                    sharedViewModel.setToggleState(false)
                    // Toggle button is off
                }
            }
        }

        // Observe the state of the shared view model and update the state of the toggle button when it changes
        sharedViewModel.getToggleState().observe(viewLifecycleOwner) { state ->
            Log.d(TAG, "observe: $state")
            toggleButton.isChecked = state ?: false
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}