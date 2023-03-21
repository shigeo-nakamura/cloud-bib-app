package com.cloudbib.client

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

data class User(val name: String, val id: Int)

data class BorrowedBook(val id: Int, val title: String)

class HttpResponse(
    val success: Boolean,
    val errorCode: Int,
    val user: User?,
    val returned_book_title: String,
    val returned_book_id: Int,
    val borrowed_book: BorrowedBook?,
    val num_borrowed_book: Int,
)

class HttpUtility(private val context: Context) {
    private val tag = "com.cloudbib.client.HttpUtility"
    private var connection: HttpURLConnection? = null
    private var urlString: String? = null
    private var userId: String? = null

    private fun makeHttpPostRequest(
        urlString: String,
        postData: String,
        cookies: Set<String>? = null
    ): HttpURLConnection? {
        val url = URL(urlString)
        val connection = url.openConnection() as? HttpURLConnection ?: return null
        connection.requestMethod = "POST"
        connection.doInput = true
        connection.doOutput = true
        connection.useCaches = false

        Log.d(tag, "urlString=$urlString, postData=$postData")

        // Set request properties
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("Connection", "keep-alive")
        if (cookies != null) {
            for (cookie in cookies) {
                connection.addRequestProperty("Cookie", cookie.split(";")[0])
            }
        }

        // Write post data to output stream
        val outputStream = connection.outputStream
        val writer = OutputStreamWriter(outputStream, StandardCharsets.UTF_8)
        writer.write(postData)
        writer.flush()
        writer.close()

        return connection
    }

    fun login(urlString: String, username: String, password: String): Boolean {
        var success = false
        this.urlString = urlString

        try {
            val postData = JSONObject().apply {
                put("uname", username)
                put("password", password)
                put("user_category", "operator")
                put("user_id", "")
                put("Content-Type", "application/json")
            }
            connection = makeHttpPostRequest(
                "$urlString/auth/login",
                postData.toString()
            ) // Assign connection object to class member variable
            val responseString = connection?.inputStream?.bufferedReader()?.readText()
            Log.d(tag, "Response: $responseString")

            // Store session cookies
            val cookies = connection?.headerFields?.get("Set-Cookie")
            if (cookies != null) {
                // Save cookies in shared preferences
                val prefs = context.getSharedPreferences("cookies", Context.MODE_PRIVATE)
                prefs.edit().putStringSet("session_cookies", cookies.toSet()).apply()
            }

            if ((connection?.responseCode
                    ?: HttpURLConnection.HTTP_OK) == HttpURLConnection.HTTP_OK
            ) {
                val jsonResponse = responseString?.let {
                    JSONObject(it)
                }
                if (jsonResponse != null) {
                    success = jsonResponse.optBoolean("success")
                }
            } else {
                Log.e(tag, "Error logging in: ${connection?.responseCode}")
            }
        } catch (e: Exception) {
            connection?.disconnect()
            Log.e(tag, "Error logging in", e)
        }

        return success
    }

    fun returnBook(book_id: String): HttpResponse {
        try {
            val postData = JSONObject().apply {
                put("returned_book_id", book_id)
                put("user_id", "")
                put("borrowed_book_id", "")
            }.toString()

            val storedCookies = getStoredCookies()
            connection = makeHttpPostRequest("$urlString/work/process", postData, storedCookies)

            // Log the response
            Log.d(tag, "Response code: ${connection?.responseCode}")
            val response = connection?.inputStream?.bufferedReader()?.readText()
            Log.d(tag, "Response body: $response")

            if (connection?.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonResponse = response?.let {
                    JSONObject(it)
                }
                if (jsonResponse != null) {
                    return if (jsonResponse.optBoolean("success")) {
                        val userJson = jsonResponse.optJSONObject("user")
                        val user = if (userJson != null) Gson().fromJson(
                            userJson.toString(),
                            User::class.java
                        ) else null
                        val returnedBookTitle = jsonResponse.getString("returned_book_title")
                        val returnedBookId = jsonResponse.getInt("returned_book_id")
                        HttpResponse(true, 0, user, returnedBookTitle, returnedBookId, null, 0)
                    } else {
                        val errorCode = jsonResponse.getInt("errcode")
                        HttpResponse(false, errorCode, null, "", 0, null, 0)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Exception return_book", e)
        }

        throw Exception("connection error")
    }

    fun selectUser(user_id: String): HttpResponse {
        try {
            val postData = JSONObject().apply {
                put("user_id", user_id)
                put("returned_book_id", "")
                put("borrowed_book_id", "")
            }.toString()

            val storedCookies = getStoredCookies()
            connection = makeHttpPostRequest("$urlString/work/process", postData, storedCookies)

            // Log the response
            Log.d(tag, "Response code: ${connection?.responseCode}")
            val response = connection?.inputStream?.bufferedReader()?.readText()
            Log.d(tag, "Response body: $response")

            if (connection?.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonResponse = response?.let {
                    JSONObject(it)
                }
                if (jsonResponse != null) {
                    return if (jsonResponse.optBoolean("success")) {
                        val userJson = jsonResponse.optJSONObject("user")
                        val user = if (userJson != null) Gson().fromJson(
                            userJson.toString(),
                            User::class.java
                        ) else null
                        var numBorrowedBook = 0
                        if (user != null) {
                            userId = user.id.toString()
                            numBorrowedBook =
                                jsonResponse.getJSONArray("borrowed_books").length()
                        }
                        HttpResponse(true, 0, user, "", 0, null, numBorrowedBook)
                    } else {
                        val errorCode = jsonResponse.getInt("errcode")
                        HttpResponse(false, errorCode, null, "", 0, null, 0)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Exception selecting user", e)
        }

        throw Exception("connection error")
    }

    fun borrowBook(book_id: String): HttpResponse {
        try {
            val postData = JSONObject().apply {
                put("user_id", userId)
                put("returned_book_id", "")
                put("borrowed_book_id", book_id)
            }.toString()

            val storedCookies = getStoredCookies()
            connection =
                makeHttpPostRequest("$urlString/work/process", postData, storedCookies)

            // Log the response
            Log.d(tag, "Response code: ${connection?.responseCode}")
            val response = connection?.inputStream?.bufferedReader()?.readText()
            Log.d(tag, "Response body: $response")

            if (connection?.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonResponse = response?.let {
                    JSONObject(it)
                }
                if (jsonResponse != null) {
                    return if (jsonResponse.optBoolean("success")) {
                        val userJson = jsonResponse.optJSONObject("user")
                        val user = if (userJson != null) Gson().fromJson(
                            userJson.toString(),
                            User::class.java
                        ) else null
                        val borrowedBookObjects =
                            jsonResponse.getJSONArray("borrowed_books")
                        val borrowedBookObject =
                            borrowedBookObjects.getJSONObject(0)
                        val borrowedBook = BorrowedBook(
                            borrowedBookObject.getInt("book_id"),
                            borrowedBookObject.getString("book_title")
                        )
                        HttpResponse(true, 0, user, "", 0, borrowedBook,  borrowedBookObjects.length())
                    } else {
                        val errorCode = jsonResponse.getInt("errcode")
                        HttpResponse(false, errorCode, null, "", 0, null, 0)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Exception borrowing book", e)
        }

        throw Exception("connection error")
    }

    private fun getStoredCookies(): Set<String>? {
        val prefs = context.getSharedPreferences("cookies", Context.MODE_PRIVATE)
        return prefs.getStringSet("session_cookies", emptySet())
    }

    fun disconnect() {
        connection?.disconnect()
    }

}