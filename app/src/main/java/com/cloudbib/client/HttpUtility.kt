import android.content.Context
import android.util.Log
import com.google.gson.Gson
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class User(val name: String, val id: Int)
class HttpResponse(
    val success: Boolean,
    val errorCode: Int,
    val user: User?,
    var returned_book_title: String,
    val returned_book_id: Int,
)

class HttpUtility(private val context: Context) {
    private val TAG = "HttpUtility"
    private var connection: HttpURLConnection? = null
    private var urlString: String? = null

    fun login(urlString: String, username: String, password: String): Boolean {
        var success = false
        this.urlString = urlString

        try {
            val url = URL("$urlString/auth/login")

            Log.d(TAG, "url: $url, username: $username")

            connection = url.openConnection() as HttpURLConnection
            connection!!.requestMethod = "POST"
            connection!!.doOutput = true

            val postData = "uname=$username&password=$password&user_category=operator&user_id="
            connection!!.outputStream.write(postData.toByteArray())

            val reader = BufferedReader(InputStreamReader(connection!!.inputStream))
            val buffer = StringBuffer()

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                buffer.append(line)
            }

            val responseString = buffer.toString()
            Log.d(TAG, "Response: $responseString")

            // Store session cookies
            val cookies = connection!!.headerFields["Set-Cookie"]
            if (cookies != null) {
                // Save cookies in shared preferences
                val prefs = context.getSharedPreferences("cookies", Context.MODE_PRIVATE)
                prefs.edit().putStringSet("session_cookies", cookies.toSet()).apply()
            }

            reader.close()

            if (connection!!.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonResponse = JSONObject(responseString)
                success = jsonResponse.optBoolean("success")
            } else {
                Log.e(TAG, "Error logging in: ${connection!!.responseCode}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error logging in", e)
        }

        return success
    }

    fun return_book(book_id: String): HttpResponse {
        try {
            val url = URL("$urlString/work/process")

            connection = url.openConnection() as HttpURLConnection
            connection!!.requestMethod = "POST"
            connection!!.doOutput = true

            // Get the stored session cookies and add them to the request headers
            val storedCookies = getStoredCookies()
            if (storedCookies != null) {
                for (cookie in storedCookies) {
                    connection!!.addRequestProperty("Cookie", cookie.split(";")[0])
                }
            }
            Log.d(TAG, storedCookies.toString())

            val postData = "returned_book_id=$book_id&user_id=&borrowed_book_id="
            connection!!.outputStream.write(postData.toByteArray())

            val reader = BufferedReader(InputStreamReader(connection!!.inputStream))
            val buffer = StringBuffer()

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                buffer.append(line)
            }

            val responseString = buffer.toString()
            Log.d(TAG, "Response: $responseString")

            reader.close()

            if (connection!!.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonResponse = JSONObject(responseString)
                if (jsonResponse.optBoolean("success")) {
                    val userJson = jsonResponse.optJSONObject("user")
                    val user = if (userJson != null) Gson().fromJson(
                        userJson.toString(),
                        User::class.java
                    ) else null
                    val returnedBookTitle = jsonResponse.getString("returned_book_title")
                    val returnedBookId = jsonResponse.getInt("returned_book_id")
                    return HttpResponse(true, 0, user, returnedBookTitle, returnedBookId)
                } else {
                    var errorCode = jsonResponse.getInt("errcode")
                    return HttpResponse(false, errorCode, null, "", 0)
                }
            } else {
                Log.e(TAG, "Error return_book: ${connection!!.responseCode}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception return_book", e)
        }

        return HttpResponse(false, 0, null, "", 0)
    }

    private fun getStoredCookies(): Set<String>? {
        val prefs = context.getSharedPreferences("cookies", Context.MODE_PRIVATE)
        return prefs.getStringSet("session_cookies", emptySet())
    }

    fun disconnect() {
        connection?.disconnect()
    }
}
