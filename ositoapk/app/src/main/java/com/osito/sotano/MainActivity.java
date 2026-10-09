package com.osito.sotano;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.View;
import android.view.WindowManager;

public class MainActivity extends Activity {
    private static final String HOME = "https://ositoyt360.github.io/Elsotanodeosito/";
    private WebView webView;
    private PermissionRequest pendingWebPermission;
    private ValueCallback<Uri[]> fileCallback;
    private static final int FILE_PICKER = 901;
    private static final int MEDIA_PERMISSION = 902;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, 0);
        getWindow().setStatusBarColor(android.graphics.Color.rgb(8,10,15));
        getWindow().setNavigationBarColor(android.graphics.Color.rgb(8,10,15));
        webView = new WebView(this);
        setContentView(webView);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setSupportMultipleWindows(false);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                String host = u.getHost() == null ? "" : u.getHost();
                if ("ositoyt360.github.io".equalsIgnoreCase(host) || host.endsWith(".github.io")) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) {}
                return true;
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> {
                    pendingWebPermission = request;
                    if (hasPermission(Manifest.permission.RECORD_AUDIO) && hasPermission(Manifest.permission.CAMERA)) {
                        request.grant(request.getResources()); pendingWebPermission = null;
                    } else {
                        String[] p = new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};
                        if (Build.VERSION.SDK_INT >= 23) requestPermissions(p, MEDIA_PERMISSION);
                    }
                });
            }
            @Override public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) { callback.invoke(origin, false, false); }
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                Intent intent = params.createIntent();
                try { startActivityForResult(intent, FILE_PICKER); return true; }
                catch (Exception e) { fileCallback = null; return false; }
            }
        });
        if (savedInstanceState == null) webView.loadUrl(HOME); else webView.restoreState(savedInstanceState);
    }

    private boolean hasPermission(String p) { return Build.VERSION.SDK_INT < 23 || checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED; }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MEDIA_PERMISSION && pendingWebPermission != null) {
            boolean audio = hasPermission(Manifest.permission.RECORD_AUDIO);
            boolean camera = hasPermission(Manifest.permission.CAMERA);
            java.util.ArrayList<String> allowed = new java.util.ArrayList<>();
            for (String r : pendingWebPermission.getResources()) {
                if (r.equals(PermissionRequest.RESOURCE_AUDIO_CAPTURE) && audio) allowed.add(r);
                if (r.equals(PermissionRequest.RESOURCE_VIDEO_CAPTURE) && camera) allowed.add(r);
            }
            if (!allowed.isEmpty()) pendingWebPermission.grant(allowed.toArray(new String[0])); else pendingWebPermission.deny();
            pendingWebPermission = null;
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_PICKER && fileCallback != null) {
            Uri[] results = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            fileCallback.onReceiveValue(results); fileCallback = null;
        }
    }
    @Override public void onBackPressed() { if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed(); }
    @Override protected void onSaveInstanceState(Bundle outState) { super.onSaveInstanceState(outState); if (webView != null) webView.saveState(outState); }
    @Override protected void onDestroy() { if (webView != null) { webView.destroy(); webView = null; } super.onDestroy(); }
}
