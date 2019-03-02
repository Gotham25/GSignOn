package com.android.gsignon

import android.app.AlertDialog
import android.app.ProgressDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PorterDuff
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.util.*


/**
 * Created by Gowtham on 28-02-2019.
 */
class SignOnActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_on)

        val clientSecretWebJson = getClientSecretWebJson()

        val clientId = clientSecretWebJson.getString("client_id")
        val authUri = clientSecretWebJson.getString("auth_uri")
        val responseType = "token"
        val oauthScope = "https://www.googleapis.com/auth/userinfo.profile"
        val redirectUri = clientSecretWebJson.getJSONArray("redirect_uris")[0] as String //get the first redirect URI
        val userAgent =
            "Mozilla/5.0 (Linux; Android 6.0.1: MotoG3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.110 Mobile Safari/537.36"
        val webViewUrl =
            "$authUri?client_id=$clientId&response_type=$responseType&scope=$oauthScope&redirect_uri=$redirectUri"

        val userInfoLayout = findViewById<TableLayout>(R.id.userInfoLayout)
        val signInBtn = findViewById<Button>(R.id.signInBtn)
        val signOutBtn = findViewById<Button>(R.id.signOutBtn)

        signInBtn.setOnClickListener {

            /******* Progress Dialog to show dialog with indeterminate spinner *******/
            var pDialog = ProgressDialog(this)
            pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
            val lProgressBar = ProgressBar(this, null, android.R.attr.progressBarStyle)
            lProgressBar.indeterminateDrawable.setColorFilter(
                resources.getColor(R.color.colorAccent),
                PorterDuff.Mode.SRC_IN
            )
            pDialog.setProgressDrawable(lProgressBar.indeterminateDrawable)
            pDialog.isIndeterminate = true
            pDialog.setMessage("Fetching user details. Please wait...")
            pDialog.setCancelable(false)

            /******* Alert Dialog to show popup dialog with webview *******/
            val alert = AlertDialog.Builder(this)
            var alertRef = AlertDialog.Builder(this).create() //ref  used later to dismiss(i.e close) the popu window

            val webView = WebView(this)
            val wrapper = LinearLayout(this)
            val keyboardHack = EditText(this)
            keyboardHack.visibility = View.GONE

            webView.settings.javaScriptEnabled = true
            webView.settings.userAgentString = userAgent
            webView.loadUrl(webViewUrl)

            webView.webViewClient = object : WebViewClient() {

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)

                    /******* Check if url is not null and if it contains access_token *******/
                    if (url != null && url.contains("access_token=")) {
                        alertRef.dismiss()

                        runOnUiThread {
                            pDialog.show() //show progress dialog
                        }

                        //stop loading webView and extract token from url
                        webView.stopLoading()
                        val queryParamsMap = parseUriQuery(url)
                        val accessToken: String = queryParamsMap.getValue("access_token")

                        //show signOut btn instead of signIn as we have logged  in
                        signInBtn.visibility = View.INVISIBLE
                        signOutBtn.visibility = View.VISIBLE

                        Thread(Runnable {
                            val response = khttp.get(
                                url = "https://www.googleapis.com/oauth2/v1/userinfo", params = mapOf(
                                    "alt" to "json",
                                    "access_token" to accessToken
                                )
                            )

                            if (response.statusCode == 200) {
                                val jsonObj = response.jsonObject
                                DownloadImageTask(findViewById(R.id.user_picture)).execute(jsonObj["picture"] as String)
                                setTextInTextView("id", jsonObj["id"] as String)
                                setTextInTextView("name", jsonObj["name"] as String)
                                setTextInTextView("given_name", jsonObj["given_name"] as String)
                                setTextInTextView("family_name", jsonObj["family_name"] as String)
                                setTextInTextView("link", jsonObj["link"] as String)
                                setTextInTextView("gender", jsonObj["gender"] as String)
                                setTextInTextView("locale", jsonObj["locale"] as String)
                            } else {
                                Toast.makeText(
                                    applicationContext,
                                    "Error occurred in getting user info!",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            //make user Info table displayed only on OK response and dismiss popup dialog
                            runOnUiThread {
                                if (response.statusCode == 200)
                                    userInfoLayout.visibility = View.VISIBLE
                                pDialog.dismiss()
                            }

                        }).start()
                    }
                }
            }

            /*
            * Normal webView in Dialog doesn't show keyboard popup when a text box in it is focused
            * As a workaround we add a editText with visibility as invisible so that,
            * the webView gains focus for keyBoard input
            */
            wrapper.orientation = LinearLayout.VERTICAL
            wrapper.addView(
                keyboardHack,
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            wrapper.addView(webView, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
            alert.setView(wrapper)
            alertRef = alert.create()
            alertRef.show()

        }

        signOutBtn.setOnClickListener {
            val alert = AlertDialog.Builder(this)
            var alertRef = AlertDialog.Builder(this).create()
            //alert.setTitle("Sign out window")

            val webView = WebView(this)
            val wrapper = LinearLayout(this)
            val keyboardHack = EditText(this)
            keyboardHack.visibility = View.GONE

            webView.settings.javaScriptEnabled = true
            webView.settings.userAgentString = userAgent
            webView.loadUrl("https://accounts.google.com/logout")

            webView.webViewClient = object : WebViewClient() {

                override fun onPageFinished(view: WebView?, url: String?) {
                    userInfoLayout.visibility = View.INVISIBLE
                    signInBtn.visibility = View.VISIBLE
                    signOutBtn.visibility = View.INVISIBLE
                    webView.stopLoading()
                    alertRef.dismiss()
                }
            }
            wrapper.orientation = LinearLayout.VERTICAL
            wrapper.addView(
                keyboardHack,
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            wrapper.addView(webView, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
            alert.setView(wrapper)
            alertRef = alert.create()
            alertRef.show()
        }

    }

    /**
     * Reads the client_secret.json file from asset folder and creates a
     * JSONObject and gets the "web" value containing JSONObject which
     * holds the client info needed for API call
     */
    private fun getClientSecretWebJson(): JSONObject {
        val clientSecretInputStream = assets.open("client_secret.json")

        val bufferedReader = BufferedReader(InputStreamReader(clientSecretInputStream, "UTF-8"))
        var tmp = bufferedReader.readLine()
        val builder = StringBuilder()

        while (tmp != null) {
            builder.append(tmp)
            tmp = bufferedReader.readLine()
        }
        clientSecretInputStream.close()

        return JSONObject(builder.toString()).getJSONObject("web")
    }

    /**
     *  This class is used to download bitmap image and set
     *  it as background for the imageView passed as argument
     */
    private class DownloadImageTask(imageView: ImageView) : AsyncTask<String, Void, Bitmap>() {

        private val imgView = imageView

        override fun doInBackground(vararg urls: String?): Bitmap {
            val urlDisplay = urls[0]
            var mIcon: Bitmap? = null
            try {
                val inputStream = URL(urlDisplay).openStream()
                mIcon = BitmapFactory.decodeStream(inputStream)
            } catch (e: Exception) {
                e.stackTrace
            }
            return mIcon!!
        }

        override fun onPostExecute(result: Bitmap?) {
            imgView.setImageBitmap(result)
        }

    }

    /**
     * This method sets the userInfo received via API call in
     * textView inside TableLayout
     *
     * Note: Since this method is called from a Non-UI thread, it can't update the
     * textView directly. Hence we need to run the statement on UI thread. Ref stackoverflow thread below
     * https://stackoverflow.com/questions/5161951/android-only-the-original-thread-that-created-a-view-hierarchy-can-touch-its-vi
     */
    private fun setTextInTextView(textViewName: String, text: String) {
        val textViewId: Int = getIdFromName(textViewName)
        runOnUiThread {
            findViewById<TextView>(textViewId).text = text
        }
    }

    /**
     * This method gives the mapping for
     * the textView name to it's viewId
     */
    private fun getIdFromName(textViewName: String): Int {
        return when (textViewName) {
            "id" -> R.id.user_id
            "name" -> R.id.user_name
            "given_name" -> R.id.user_given_name
            "family_name" -> R.id.user_family_name
            "link" -> R.id.user_link
            "picture" -> R.id.user_picture
            "gender" -> R.id.user_gender
            "locale" -> R.id.user_locale
            else -> 0
        }
    }

    /**
     * This method is used to get a map of queryParams from the url
     *
     */
    fun parseUriQuery(uri: String): Map<String, String> {
        val queryParamMap = HashMap<String, String>()
        val urlParts = uri.split("\\?|#".toRegex()) //splits on '?' (or) '#' as the queryParams in url comes after it
        if (urlParts.size > 1) {
            val queryString = urlParts[1]
            val queryStringParts =
                queryString.split("&".toRegex()) // splits on '&' as each param pairs are separated by ampersand
            queryStringParts/*.asList()*/.forEach {
                val pair = it.split("=".toRegex()) //splits on '=' as the param pair is delimited by equal symbol
                if (pair.size > 1) {
                    queryParamMap[pair[0]] = pair[1]
                }
            }
        }
        return queryParamMap
    }
}