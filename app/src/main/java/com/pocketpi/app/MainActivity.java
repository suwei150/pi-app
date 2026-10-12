package com.pocketpi.app;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class MainActivity extends Activity {
    private WebView web;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        web.addJavascriptInterface(new Api(), "native");
        web.setBackgroundColor(0xFF0B0E14);
        web.loadUrl("file:///android_asset/index.html");
        setContentView(web);
    }

    public class Api {
        @JavascriptInterface
        public String version() {
            return "pocket-pi 0.1.0-m1";
        }

        @JavascriptInterface
        public String echo(String x) {
            return "native said: " + x;
        }
    }
}
