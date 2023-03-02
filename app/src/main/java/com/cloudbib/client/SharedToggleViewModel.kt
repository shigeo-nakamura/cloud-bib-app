package com.cloudbib.client

import HttpUtility
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SharedToggleViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "SharedToggle"
    private var url: String? = null
    private var username: String? = null
    private var password: String? = null
    private var httpUtility: HttpUtility = HttpUtility(getApplication())
    private val toggleState = MutableLiveData(false)
    private val loginError = MutableLiveData<String>()

    init {
        setDefaultValues()
    }

    fun getToggleState(): LiveData<Boolean> {
        return toggleState
    }

    // Expose httpUtility as a public property
    fun getHttpUtility(): HttpUtility {
        return httpUtility
    }

    fun getLoginError(): LiveData<String> {
        return loginError
    }

    fun setToggleState(state: Boolean) {

        viewModelScope.launch {
            if (state) {
                Log.d(TAG, "connect")
                val loginSuccess = withContext(Dispatchers.IO) {
                    url != null && username != null && password != null && httpUtility.login(
                        url!!,
                        username!!,
                        password!!
                    )
                }
                if (loginSuccess) {
                    toggleState.value = true
                    return@launch
                } else {
                    // Notify the activity or fragment of the login error
                    loginError.value = "ログインできませんでした"
                }
            } else {
                Log.d(TAG, "disconnect")
                httpUtility.disconnect()
            }
            toggleState.value = false
        }
    }

    fun setLoginCredentials(url: String, username: String, password: String) {
        this.url = url
        this.username = username
        this.password = password
    }

    private fun setDefaultValues() {
        val sharedPref = getApplication<Application>().applicationContext.getSharedPreferences(
            "my_prefs",
            Context.MODE_PRIVATE
        )
        url = sharedPref.getString("server", null)
        username = sharedPref.getString("userName", null)
        password = sharedPref.getString("password", null)

        // If nothing is saved in preferences, set the default values
        if (url == null || username == null || password == null) {
            url = "https://www.cloudbib.net"
            username = "demo"
            password = "demo"
        }
    }
}
