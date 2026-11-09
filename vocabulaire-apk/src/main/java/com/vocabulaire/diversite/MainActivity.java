package com.vocabulaire.diversite;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        // localStorage : la progression reste enregistrée entre deux ouvertures de l'app
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);

        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    public void onBackPressed() {
        // Pendant une leçon, le bouton retour ramène à l'accueil au lieu de quitter l'app
        webView.evaluateJavascript(
                "(function(){ if (typeof L !== 'undefined' && L) { L = null; render(); return true; } return false; })();",
                value -> {
                    if (!"true".equals(value)) {
                        super.onBackPressed();
                    }
                });
    }
}
