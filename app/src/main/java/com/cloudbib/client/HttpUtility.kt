import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class HttpUtility(private val context: Context) {
    private var connection: HttpURLConnection? = null

    fun login(urlString: String, username: String, password: String): Boolean {
        var success = false

        try {
            val url = URL("$urlString/auth/login")

            Log.d("HttpUtility", "url: $url, username: $username")

            connection = url.openConnection() as HttpURLConnection
            connection!!.requestMethod = "POST"
            connection!!.doOutput = true

            val postData = "uname=$username&password=$password&user_category=operator&user_id=0"
            connection!!.outputStream.write(postData.toByteArray())

            val reader = BufferedReader(InputStreamReader(connection!!.inputStream))
            val buffer = StringBuffer()

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                buffer.append(line)
            }

            val responseString = buffer.toString()
            Log.d("HttpUtility", "Response: $responseString")

            // Store session cookies
            val cookies = connection!!.headerFields["Set-Cookie"]
            if (cookies != null) {
                // Save cookies in shared preferences
                val prefs = context.getSharedPreferences("cookies", Context.MODE_PRIVATE)
                prefs.edit().putStringSet("session_cookies", cookies.toSet()).apply()
            }

            reader.close()

            if (connection!!.responseCode == HttpURLConnection.HTTP_OK) {
                success = true
            } else {
                Log.e("HttpUtility", "Error logging in: ${connection!!.responseCode}")
            }
        } catch (e: Exception) {
            Log.e("HttpUtility", "Error logging in", e)
        }

        return success
    }

    fun post(urlString: String, data: String): JSONObject {
        var response = JSONObject()

        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection!!.requestMethod = "POST"
            connection!!.doOutput = true

            val cookies = getStoredCookies()
            if (!cookies.isNullOrEmpty()) {
                connection!!.setRequestProperty("Cookie", cookies.joinToString(";"))
            }

            connection!!.outputStream.write(data.toByteArray())

            val reader = BufferedReader(InputStreamReader(connection!!.inputStream))
            val buffer = StringBuffer()

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                buffer.append(line)
            }

            val responseString = buffer.toString()

            reader.close()

            if (connection!!.responseCode == HttpURLConnection.HTTP_OK) {
                response = JSONObject(responseString)
            } else {
                Log.e("HttpUtility", "Error posting data: ${connection!!.responseCode}")
            }
        } catch (e: Exception) {
            Log.e("HttpUtility", "Error posting data", e)
        }

        return response
    }

    private fun getStoredCookies(): Set<String>? {
        val prefs = context.getSharedPreferences("cookies", Context.MODE_PRIVATE)
        return prefs.getStringSet("session_cookies", emptySet())
    }

    fun disconnect() {
        connection?.disconnect()
    }
}
