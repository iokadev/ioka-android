package kz.ioka.android.ioka.presentation.webView

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import com.google.android.material.appbar.MaterialToolbar
import kz.ioka.android.ioka.R
import kz.ioka.android.ioka.viewBase.BaseActivity


class WebViewActivity : BaseActivity() {

    private var launcher: WebViewLauncher? = null

    private lateinit var vToolbar: MaterialToolbar
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)

        launcher = launcher()
        bindViews()
        setupViews()
    }

    private fun bindViews() {
        vToolbar = findViewById(R.id.vToolbar)
        webView = findViewById(R.id.webView)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupViews() {
        vToolbar.title = launcher?.toolbarTitle

        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                if (url.startsWith("https://stage-checkout.ioka.kz/customers/")) {
                    setResult(RESULT_OK)
                    finish()
                }

                return url.startsWith("https://stage-checkout.ioka.kz/customers/")
            }
        }
        webView.loadUrl(launcher?.actionUrl ?: "https://ioka.kz")
    }

}