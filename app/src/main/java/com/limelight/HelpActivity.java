package com.limelight;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.limelight.utils.SpinnerDialog;

import java.util.Arrays;

public class HelpActivity extends AppCompatActivity {
    // DER-encoded certificate the page may present instead of a CA-signed one. The host's
    // server config page uses the same self-signed certificate the host paired with.
    public static final String EXTRA_TRUSTED_CERT = "trustedCert";

    private SpinnerDialog loadingDialog;
    private byte[] trustedCert;
    private WebView webView;

    private boolean backCallbackRegistered;
    private OnBackInvokedCallback onBackInvokedCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedCallback = new OnBackInvokedCallback() {
                @Override
                public void onBackInvoked() {
                    // We should always be able to go back because we unregister our callback
                    // when we can't go back. Nonetheless, we will still check anyway.
                    if (webView.canGoBack()) {
                        webView.goBack();
                    }
                }
            };
        }

        trustedCert = getIntent().getByteArrayExtra(EXTRA_TRUSTED_CERT);

        webView = new WebView(this);
        setContentView(webView);

        // These allow the user to zoom the page
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);

        // This sets the view to display the whole page by default
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(true);

        // This allows the links to places on the same page to work
        webView.getSettings().setJavaScriptEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (loadingDialog == null) {
                    loadingDialog = SpinnerDialog.displayDialog(HelpActivity.this,
                            getResources().getString(R.string.help_loading_title),
                            getResources().getString(R.string.help_loading_msg), false);
                }

                refreshBackDispatchState();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (loadingDialog != null) {
                    loadingDialog.dismiss();
                    loadingDialog = null;
                }

                refreshBackDispatchState();
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                if (trustedCert != null && Arrays.equals(trustedCert, encoded(error.getCertificate()))) {
                    handler.proceed();
                }
                else {
                    handler.cancel();
                    closeWithError(R.string.help_error_certificate);
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    closeWithError(R.string.help_error_unreachable);
                }
            }
        });

        webView.loadUrl(getIntent().getData().toString());
    }

    private static byte[] encoded(SslCertificate certificate) {
        if (certificate == null) {
            return null;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return certificate.getX509Certificate().getEncoded();
            }
            return SslCertificate.saveState(certificate).getByteArray("x509-certificate");
        } catch (Exception e) {
            return null;
        }
    }

    // A page that fails to load leaves an empty, black screen, so say why and go back
    private void closeWithError(int messageId) {
        if (isFinishing()) {
            return;
        }
        if (loadingDialog != null) {
            loadingDialog.dismiss();
            loadingDialog = null;
        }
        Toast.makeText(this, messageId, Toast.LENGTH_LONG).show();
        finish();
    }

    private void refreshBackDispatchState() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (webView.canGoBack() && !backCallbackRegistered) {
                getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                        OnBackInvokedDispatcher.PRIORITY_DEFAULT, onBackInvokedCallback);
                backCallbackRegistered = true;
            }
            else if (!webView.canGoBack() && backCallbackRegistered) {
                getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(onBackInvokedCallback);
                backCallbackRegistered = false;
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (backCallbackRegistered) {
                getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(onBackInvokedCallback);
            }
        }

        super.onDestroy();
    }

    @Override
    // NOTE: This will NOT be called on Android 13+ with android:enableOnBackInvokedCallback="true"
    public void onBackPressed() {
        // Back goes back through the WebView history
        // until no more history remains
        if (webView.canGoBack()) {
            webView.goBack();
        }
        else {
            super.onBackPressed();
        }
    }
}
