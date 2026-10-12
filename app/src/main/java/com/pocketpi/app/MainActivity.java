package com.pocketpi.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MainActivity extends Activity {
    private WebView web;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        WebView.setWebContentsDebuggingEnabled(true);
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        web.addJavascriptInterface(new Api(), "native");
        web.setBackgroundColor(0xFF0B0E14);
        web.setWebChromeClient(new WebChromeClient());
        web.loadUrl("file:///android_asset/index.html");
        setContentView(web);
    }

    public class Api {
        @JavascriptInterface
        public String version() {
            return "pocket-pi 0.2.0-m1.5";
        }

        @JavascriptInterface
        public String echo(String x) {
            return "native said: " + x;
        }

        @JavascriptInterface
        public String shell(String cmd) {
            StringBuilder out = new StringBuilder();
            try {
                Process p = Runtime.getRuntime().exec(new String[]{"/system/bin/sh", "-c", cmd});
                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                while ((line = br.readLine()) != null) out.append(line).append("\n");
                br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                while ((line = br.readLine()) != null) out.append("[ERR] ").append(line).append("\n");
                p.waitFor();
                if (out.length() == 0) out.append("(exit: ").append(p.exitValue()).append(")");
            } catch (Exception e) {
                out.append("shell err: ").append(e.getMessage());
            }
            return out.toString().trim();
        }

        @JavascriptInterface
        public String sysinfo() {
            try {
                JSONObject j = new JSONObject();
                j.put("brand", Build.BRAND);
                j.put("model", Build.MODEL);
                j.put("sdk", Build.VERSION.SDK_INT);
                j.put("release", Build.VERSION.RELEASE);
                j.put("device", Build.DEVICE);
                j.put("manufacturer", Build.MANUFACTURER);
                return j.toString();
            } catch (Exception e) {
                return "{}";
            }
        }

        @JavascriptInterface
        public void toast(String msg) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show());
        }

        @JavascriptInterface
        public void clip(String text) {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("pi", text));
        }

        @JavascriptInterface
        public String execJS(String code) {
            final String[] result = {""};
            runOnUiThread(() -> {
                web.evaluateJavascript(code, value -> {
                    if (value != null) result[0] = value;
                });
            });
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            return result[0];
        }
    }
}
