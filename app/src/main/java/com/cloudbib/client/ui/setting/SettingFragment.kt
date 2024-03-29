package com.cloudbib.client.ui.setting

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.cloudbib.client.SharedToggleViewModel
import com.cloudbib.client.databinding.FragmentSettingBinding
import com.google.android.material.snackbar.Snackbar

class SettingFragment : Fragment() {

    private val tag = "SettingFragment"
    private var _binding: FragmentSettingBinding? = null
    private lateinit var settingViewModel: SettingViewModel

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private fun setDefaultValues(binding: FragmentSettingBinding) {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)
        val savedUserName = sharedPref?.getString("userName", "demo")
        val savedPassword = sharedPref?.getString("password", "demo")
        val savedServer = sharedPref?.getString("server", "https://www.cloudbib.net")

        binding.userName.setText(savedUserName)
        binding.password.setText(savedPassword)
        binding.server.setText(savedServer)

        if (savedServer != null && savedUserName != null && savedPassword != null) {
            val sharedViewModel = ViewModelProvider(requireActivity())[SharedToggleViewModel::class.java]
            sharedViewModel.setLoginCredentials(savedServer, savedUserName, savedPassword)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        settingViewModel = ViewModelProvider(this)[SettingViewModel::class.java]

        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setDefaultValues(binding)

        val saveButton = binding.buttonSave
        saveButton.setOnClickListener {
            val userName = binding.userName.text.toString()
            val password = binding.password.text.toString()
            val server = binding.server.text.toString()

            // Save the values to SharedPreferences or database
            // For example, to save to SharedPreferences:
            with (activity?.getPreferences(Context.MODE_PRIVATE)?.edit()) {
                this?.putString("userName", userName)
                this?.putString("password", password)
                this?.putString("server", server)
                this?.apply()
            }

            val sharedViewModel = ViewModelProvider(requireActivity())[SharedToggleViewModel::class.java]
            sharedViewModel.setLoginCredentials(server, userName, password)

            Log.d(tag, "saved")

            // Show a Snack bar to indicate that the settings were saved
            Snackbar.make(view ?: return@setOnClickListener, "保存しました", Snackbar.LENGTH_SHORT).show()
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
