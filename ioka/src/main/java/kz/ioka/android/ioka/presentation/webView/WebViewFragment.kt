package kz.ioka.android.ioka.presentation.webView

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import kz.ioka.android.ioka.R
import kz.ioka.android.ioka.viewBase.BaseFragment

internal class WebViewFragment : BaseFragment(R.layout.ioka_fragment_web_view) {

    companion object {
        const val WEB_VIEW_REQUEST_KEY = "WEB_VIEW_REQUEST_KEY"
        const val WEB_VIEW_RESULT_BUNDLE_KEY = "WEB_VIEW_RESULT_BUNDLE_KEY"

        const val REDIRECT_URL = "https://ioka.kz/"

        const val RESULT_SUCCESS = 1_000
        const val RESULT_FAIL = 1_001

        fun getInstance(
            behavior: WebViewBehavior
        ): WebViewFragment {
            val bundle = Bundle()
            bundle.putParcelable(FRAGMENT_LAUNCHER, behavior)

            val fragment = WebViewFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    private val viewModel: WebViewViewModel by viewModels {
        WebViewViewModelFactory(
            launcher()!!
        )
    }

    private var launcher: WebViewBehavior? = null

    private lateinit var vToolbar: Toolbar
    private lateinit var vToolbarTitle: AppCompatTextView
    private lateinit var webView: WebView
    private lateinit var vProgress: FrameLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        launcher = launcher()
        bindViews(view)
        setupViews()
        observeData()
    }

    private fun bindViews(view: View) {
        vToolbar = view.findViewById(R.id.vToolbar)
        vToolbarTitle = view.findViewById(R.id.tvToolbarTitle)
        webView = view.findViewById(R.id.webView)
        vProgress = view.findViewById(R.id.vProgress)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupViews() {
        vToolbar.setNavigationOnClickListener {
            val data = Bundle()
            data.putParcelable(WEB_VIEW_RESULT_BUNDLE_KEY, ResultState.Canceled)

            setFragmentResult(WEB_VIEW_REQUEST_KEY, data)
            parentFragmentManager.popBackStack()
        }

        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                if (url == REDIRECT_URL) {
                    viewModel.onRedirected()
                }

                return false
            }
        }
    }

    private fun observeData() {
        viewModel.apply {
            vToolbarTitle.text = getString(toolbarTitleRes)
            webView.loadUrl(actionUrl)

            progress.observe(viewLifecycleOwner) {
                vProgress.isVisible = it
            }

            result.observe(viewLifecycleOwner) {
                val data = Bundle()
                data.putParcelable(WEB_VIEW_RESULT_BUNDLE_KEY, it)

                setFragmentResult(WEB_VIEW_REQUEST_KEY, data)
                parentFragmentManager.popBackStack()
            }
        }
    }

    override fun onDestroy() {
        webView.clearCache(true)
        webView.clearFormData()
        webView.clearHistory()
        webView.clearSslPreferences()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        WebStorage.getInstance().deleteAllData()

        super.onDestroy()
    }

}