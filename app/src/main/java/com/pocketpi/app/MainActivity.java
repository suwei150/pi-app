package com.pocketpi.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

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
            return "pocket-pi 0.3.0-m2";
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
        public void shellStream(final String cmd) {
            final long id = System.currentTimeMillis();
            new Thread(() -> {
                String jsStart = "(function(){if(!window.termStreams)window.termStreams={};window.termStreams[" + id + "]={done:false,output:''};window.termOnStream('start'," + id + ",'" + jsEscape(cmd) + "');})()";
                web.evaluateJavascript(jsStart, null);
                try {
                    Process p = Runtime.getRuntime().exec(new String[]{"/system/bin/sh", "-c", cmd});
                    BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    String line;
                    while ((line = br.readLine()) != null) {
                        String js = "(function(){window.termOnStream('stdout'," + id + ",'" + jsEscape(line) + "');})()";
                        web.evaluateJavascript(js, null);
                    }
                    br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                    while ((line = br.readLine()) != null) {
                        String js = "(function(){window.termOnStream('stderr'," + id + ",'" + jsEscape(line) + "');})()";
                        web.evaluateJavascript(js, null);
                    }
                    p.waitFor();
                    int exit = p.exitValue();
                    String jsEnd = "(function(){window.termOnStream('exit'," + id + "," + exit + ");})()";
                    web.evaluateJavascript(jsEnd, null);
                } catch (Exception e) {
                    String jsErr = "(function(){window.termOnStream('exit'," + id + ",-1);window.termOnStream('stderr'," + id + ",'" + jsEscape(e.getMessage()) + "');})()";
                    web.evaluateJavascript(jsErr, null);
                }
            }).start();
        }

        private String jsEscape(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
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

        @JavascriptInterface
        public String http(String urlStr) {
            StringBuilder out = new StringBuilder();
            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setInstanceFollowRedirects(true);
                int code = conn.getResponseCode();
                out.append("HTTP ").append(code).append("\n");
                if (code >= 200 && code < 300) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String line;
                    int lineCount = 0;
                    while ((line = br.readLine()) != null && lineCount < 50) {
                        out.append(line).append("\n");
                        lineCount++;
                    }
                    if (lineCount >= 50) out.append("... (truncated at 50 lines)\n");
                } else {
                    try {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                        String line;
                        while ((line = br.readLine()) != null) out.append(line).append("\n");
                    } catch (Exception ignored) {}
                }
                conn.disconnect();
            } catch (Exception e) {
                out.append("http err: ").append(e.getMessage());
            }
            return out.toString().trim();
        }

        @JavascriptInterface
        public String fileList(String dir) {
            StringBuilder out = new StringBuilder();
            try {
                File d = new File(dir);
                if (!d.exists() || !d.isDirectory()) {
                    out.append("not a dir: ").append(dir);
                    return out.toString();
                }
                File[] files = d.listFiles();
                if (files == null || files.length == 0) {
                    out.append("(empty)");
                    return out.toString();
                }
                for (File f : files) {
                    String type = f.isDirectory() ? "[D]" : "[F]";
                    String size = f.isDirectory() ? "-" : formatSize(f.length());
                    out.append(type).append(" ").append(size).append("  ").append(f.getName()).append("\n");
                }
            } catch (Exception e) {
                out.append("err: ").append(e.getMessage());
            }
            return out.toString();
        }

        @JavascriptInterface
        public String readFile(String path) {
            try {
                File f = new File(path);
                if (!f.exists()) return "file not found: " + path;
                if (f.length() > 102400) return "file too large (>100KB): " + formatSize(f.length());
                StringBuilder sb = new StringBuilder();
                FileInputStream fis = new FileInputStream(f);
                byte[] buf = new byte[4096];
                int n;
                while ((n = fis.read(buf)) != -1) {
                    sb.append(new String(buf, 0, n, "UTF-8"));
                }
                fis.close();
                return sb.toString();
            } catch (Exception e) {
                return "read err: " + e.getMessage();
            }
        }

        @JavascriptInterface
        public void writeFile(String path, String content) {
            try {
                File f = new File(path);
                File parent = f.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                FileOutputStream fos = new FileOutputStream(f);
                fos.write(content.getBytes("UTF-8"));
                fos.close();
                toast("Written: " + path);
            } catch (Exception e) {
                toast("Write err: " + e.getMessage());
            }
        }

        @JavascriptInterface
        public String battery() {
            try {
                IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                Intent battery = registerReceiver(null, filter);
                JSONObject j = new JSONObject();
                if (battery != null) {
                    j.put("level", battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1));
                    j.put("scale", battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1));
                    j.put("status", battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1));
                    j.put("temp", battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10);
                    int status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                    String statusStr;
                    switch (status) {
                        case BatteryManager.BATTERY_STATUS_CHARGING: statusStr = "charging"; break;
                        case BatteryManager.BATTERY_STATUS_FULL: statusStr = "full"; break;
                        case BatteryManager.BATTERY_STATUS_DISCHARGING: statusStr = "discharging"; break;
                        case BatteryManager.BATTERY_STATUS_NOT_CHARGING: statusStr = "not_charging"; break;
                        default: statusStr = "unknown"; break;
                    }
                    j.put("statusStr", statusStr);
                }
                return j.toString();
            } catch (Exception e) {
                return "{}";
            }
        }

        @JavascriptInterface
        public String storage() {
            try {
                File[] roots = getExternalFilesDirs(null);
                JSONObject j = new JSONObject();
                if (roots != null && roots.length > 0) {
                    long total = roots[0].getTotalSpace();
                    long free = roots[0].getFreeSpace();
                    long used = total - free;
                    j.put("total", formatSize(total));
                    j.put("free", formatSize(free));
                    j.put("used", formatSize(used));
                    j.put("path", roots[0].getPath());
                }
                return j.toString();
            } catch (Exception e) {
                return "{}";
            }
        }

        private String formatSize(long bytes) {
            if (bytes < 1024) return bytes + "B";
            if (bytes < 1048576) return String.format("%.1fKB", bytes / 1024.0);
            if (bytes < 1073741824) return String.format("%.1fMB", bytes / 1048576.0);
            return String.format("%.1fGB", bytes / 1073741824.0);
        }
    }
}