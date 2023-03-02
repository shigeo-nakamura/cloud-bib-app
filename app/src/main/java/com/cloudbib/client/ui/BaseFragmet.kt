import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

abstract class BaseFragment : Fragment() {
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private var activityResultCallback: ((ActivityResult) -> Unit)? = null

    companion object {
        const val RESULT_OK = -1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            activityResultCallback?.invoke(result)
        }
    }

    fun startActivityForResult(intent: Intent, callback: (ActivityResult) -> Unit) {
        activityResultCallback = callback
        activityResultLauncher.launch(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        onActivityResult(requestCode, resultCode, data)
    }
}
