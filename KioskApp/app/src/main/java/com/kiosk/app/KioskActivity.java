package com.kiosk.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Button;

public class KioskActivity extends Activity {

    // =========================================================
    //  CONFIGURATION — edit these values before building
    // =========================================================
    private static final String KIOSK_URL      = "https://www.agdisplays.com/";   // <-- Set your URL
    private static final boolean LOCK_TO_URL   = true;   // prevent navigation away from domain
    private static final int     RELOAD_DELAY  = 5000;   // ms before retrying on error
    private static final boolean SHOW_PROGRESS = true;   // show loading bar
    // =========================================================

    private WebView     mWebView;
    private ProgressBar mProgressBar;
    private FrameLayout mErrorLayout;
    private TextView    mErrorText;
    private Handler     mHandler;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHandler = new Handler(Looper.getMainLooper());

        // Keep screen on at all times
        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );

        setContentView(R.layout.activity_kiosk);

        mWebView     = findViewById(R.id.webview);
        mProgressBar = findViewById(R.id.progressBar);
        mErrorLayout = findViewById(R.id.errorLayout);
        mErrorText   = findViewById(R.id.errorText);
        Button retryButton = findViewById(R.id.retryButton);

        // Enable full-screen immersive mode
        hideSystemUI();

        // Configure WebView
        configureWebView();

        // Retry button in error screen
        retryButton.setOnClickListener(v -> loadKioskUrl());

        // Load the kiosk URL
       // ----------------------------------------------------------
    //  Load (or reload) the kiosk URL — waits for Wi-Fi
    // ----------------------------------------------------------
    private void loadKioskUrl() {
        mErrorLayout.setVisibility(View.GONE);

        if (isNetworkAvailable()) {
            mWebView.loadUrl(KIOSK_URL);
        } else {
            showError("Waiting for Wi-Fi connection...");
            // Check every 3 seconds until Wi-Fi is available
            mHandler.postDelayed(this::loadKioskUrl, 3000);
        }
    }

    // ----------------------------------------------------------
    //  Full-screen immersive (hides nav bar & status bar)
    // ----------------------------------------------------------
    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }

    // ----------------------------------------------------------
    //  WebView configuration
    // ----------------------------------------------------------
    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        WebSettings settings = mWebView.getSettings();

        // JavaScript & storage
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        // Keyboard / form support
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(false);

        // Zoom disabled (kiosk should be fixed layout)
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        // Caching for offline resilience
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setAppCacheEnabled(true);

        // Media & hardware acceleration
        settings.setMediaPlaybackRequiresUserGesture(false);
        mWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        // Allow cookies
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(mWebView, true);

        // User agent (identifies as Chrome on Android)
        settings.setUserAgentString(
            "Mozilla/5.0 (Linux; Android 11; Kiosk) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/114.0.0.0 Mobile Safari/537.36");

        // WebViewClient: intercepts page navigation
        mWebView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (LOCK_TO_URL) {
                    String url = request.getUrl().toString();
                    String targetHost = android.net.Uri.parse(KIOSK_URL).getHost();
                    String requestHost = request.getUrl().getHost();
                    // Allow navigation only within same domain
                    if (requestHost != null && targetHost != null
                            && requestHost.contains(targetHost.replace("www.", ""))) {
                        return false; // let WebView handle it
                    }
                    return true; // block external links
                }
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                mErrorLayout.setVisibility(View.GONE);
                if (SHOW_PROGRESS) mProgressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                mProgressBar.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                                        WebResourceError error) {
                if (request.isForMainFrame()) {
                    mProgressBar.setVisibility(View.GONE);
                    showError("Connection error. Retrying in " + (RELOAD_DELAY / 1000) + "s…");
                    mHandler.postDelayed(() -> loadKioskUrl(), RELOAD_DELAY);
                }
            }
        });

        // WebChromeClient: handles progress bar & fullscreen video
        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (SHOW_PROGRESS) mProgressBar.setProgress(newProgress);
            }
        });
    }

    // ----------------------------------------------------------
    //  Load (or reload) the kiosk URL
    // ----------------------------------------------------------
    private void loadKioskUrl() {
        mErrorLayout.setVisibility(View.GONE);
        if (isNetworkAvailable()) {
            mWebView.loadUrl(KIOSK_URL);
        } else {
            showError("No internet connection. Retrying in " + (RELOAD_DELAY / 1000) + "s…");
            mHandler.postDelayed(() -> loadKioskUrl(), RELOAD_DELAY);
        }
    }

    // ----------------------------------------------------------
    //  Show the offline / error overlay
    // ----------------------------------------------------------
    private void showError(String message) {
        mErrorText.setText(message);
        mErrorLayout.setVisibility(View.VISIBLE);
    }

    // ----------------------------------------------------------
    //  Network check
    // ----------------------------------------------------------
    private boolean isNetworkAvailable() {
        ConnectivityManager cm =
            (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    // ----------------------------------------------------------
    //  Block all hardware keys (back, home, recents, volume)
    // ----------------------------------------------------------
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                // Optionally allow back within site; block fully if desired
                if (mWebView.canGoBack()) {
                    mWebView.goBack();
                }
                return true;
            case KeyEvent.KEYCODE_HOME:
            case KeyEvent.KEYCODE_APP_SWITCH:
            case KeyEvent.KEYCODE_MENU:
                return true; // block
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    // ----------------------------------------------------------
    //  Lifecycle
    // ----------------------------------------------------------
    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
        mWebView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mWebView.onPause();
    }

    @Override
    protected void onDestroy() {
        mWebView.destroy();
        mHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    // Re-apply immersive mode if system UI appears (e.g. user swipes)
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUI();
    }
}
