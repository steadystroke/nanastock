package io.github.steadystroke.stock;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.DisplayCutout;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

/**
 * 사이트 하나를 전체화면 WebView 로 보여 주는 앱.
 * 다른 주소를 보여 주고 싶으면 START_URL 과 APP_HOST 만 바꾸면 된다.
 */
public class MainActivity extends Activity {

    /** 앱이 처음 여는 주소 */
    private static final String START_URL = "https://steadystroke.github.io/stock/";
    /** 앱 안에서 계속 보여 줄 사이트. 이 밖의 주소는 기본 브라우저로 넘긴다. */
    private static final String APP_HOST = "steadystroke.github.io";
    /** Google 로그인 창 주소. 웹뷰에서는 Google 이 로그인을 막기 때문에 따로 안내한다. */
    private static final String GOOGLE_LOGIN_HOST = "accounts.google.com";

    private FrameLayout root;
    private WebView webView;
    private View errorView;
    private boolean loadFailed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 카메라 구멍(노치) 자리에 남는 띠는 검은색으로 둔다.
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        webView = new WebView(this);
        webView.setBackgroundColor(getColor(R.color.app_bg));
        root.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        errorView = buildErrorView();
        errorView.setVisibility(View.GONE);
        root.addView(errorView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(root);
        setUpFullscreen();
        setUpWebView();

        if (savedInstanceState == null || webView.restoreState(savedInstanceState) == null) {
            webView.loadUrl(START_URL);
        }
    }

    // ------------------------------------------------------------------
    // 전체화면
    // ------------------------------------------------------------------

    private void setUpFullscreen() {
        Window window = getWindow();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            window.setAttributes(lp);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
        }

        // 화면 끝까지 그리되, 카메라 구멍과 키보드에 가려지는 만큼만 안쪽으로 들인다.
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int left = 0, top = 0, right = 0, bottom = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Insets cutout = insets.getInsets(WindowInsets.Type.displayCutout());
                Insets keyboard = insets.getInsets(WindowInsets.Type.ime());
                left = cutout.left;
                top = cutout.top;
                right = cutout.right;
                bottom = Math.max(cutout.bottom, keyboard.bottom);
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    DisplayCutout cutout = insets.getDisplayCutout();
                    if (cutout != null) {
                        left = cutout.getSafeInsetLeft();
                        top = cutout.getSafeInsetTop();
                        right = cutout.getSafeInsetRight();
                        bottom = cutout.getSafeInsetBottom();
                    }
                }
                bottom = Math.max(bottom, legacyBottomInset(insets));
            }
            v.setPadding(left, top, right, bottom);
            return consume(insets);
        });

        hideSystemBars();
    }

    /** 안드로이드 10 이하: 키보드가 올라오면 아래쪽 여백으로 전달된다. */
    @SuppressWarnings("deprecation")
    private static int legacyBottomInset(WindowInsets insets) {
        return insets.getSystemWindowInsetBottom();
    }

    @SuppressWarnings("deprecation")
    private static WindowInsets consume(WindowInsets insets) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return WindowInsets.CONSUMED;
        }
        return insets.consumeSystemWindowInsets();
    }

    /** 상태 표시줄과 내비게이션 바를 숨긴다. 화면 가장자리를 쓸면 잠깐 나타났다가 다시 사라진다. */
    @SuppressWarnings("deprecation")
    private void hideSystemBars() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                controller.hide(WindowInsets.Type.systemBars());
            }
        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // 대화상자나 다른 앱에서 돌아오면 시스템 바가 다시 보일 수 있어 한 번 더 숨긴다.
        if (hasFocus) {
            hideSystemBars();
        }
    }

    // ------------------------------------------------------------------
    // WebView
    // ------------------------------------------------------------------

    @SuppressLint("SetJavaScriptEnabled")
    private void setUpWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);   // 사이트가 localStorage 에 포트폴리오·설정을 저장한다
        settings.setDatabaseEnabled(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        webView.setWebChromeClient(new WebChromeClient());   // alert/confirm 대화상자 표시용
        webView.setWebViewClient(new AppWebViewClient());
    }

    private final class AppWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            if (!request.isForMainFrame()) {
                return false;
            }
            Uri uri = request.getUrl();
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);

            switch (scheme) {
                case "about":
                case "data":
                case "blob":
                case "javascript":
                    return false;
                case "https":
                    if (APP_HOST.equals(host)) {
                        return false;                 // 우리 사이트는 앱 안에서 연다
                    }
                    if (GOOGLE_LOGIN_HOST.equals(host)) {
                        showLoginNotice();            // 웹뷰에서는 Google 로그인이 막혀 있다
                        return true;
                    }
                    break;
                default:
                    break;
            }
            openExternally(uri);                      // 그 밖의 링크는 기본 브라우저/앱으로
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            loadFailed = false;
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            if (request.isForMainFrame()) {
                loadFailed = true;
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            showError(loadFailed);
        }

        @Override
        public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
            // 렌더러가 죽은 WebView 는 다시 쓸 수 없다. 버리고 화면을 새로 만든다.
            destroyWebView();
            recreate();
            return true;
        }
    }

    private void openExternally(Uri uri) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (RuntimeException e) {
            Toast.makeText(this, R.string.cannot_open_link, Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoginNotice() {
        if (isFinishing()) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.login_title)
                .setMessage(R.string.login_message)
                .setPositiveButton(R.string.open_in_browser,
                        (dialog, which) -> openExternally(Uri.parse(START_URL)))
                .setNegativeButton(R.string.close, null)
                .show();
    }

    // ------------------------------------------------------------------
    // 연결 실패 화면
    // ------------------------------------------------------------------

    private View buildErrorView() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setBackgroundColor(getColor(R.color.app_bg));
        box.setClickable(true);
        int pad = dp(32);
        box.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText(R.string.error_title);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        title.setTextColor(getColor(R.color.app_text));
        title.setGravity(Gravity.CENTER);
        box.addView(title, wrap(0));

        TextView message = new TextView(this);
        message.setText(R.string.error_message);
        message.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        message.setTextColor(getColor(R.color.app_text_2));
        message.setGravity(Gravity.CENTER);
        box.addView(message, wrap(dp(8)));

        Button retry = new Button(this);
        retry.setText(R.string.retry);
        retry.setOnClickListener(v -> retryLoad());
        box.addView(retry, wrap(dp(24)));

        return box;
    }

    private static LinearLayout.LayoutParams wrap(int topMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = topMargin;
        return lp;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void showError(boolean failed) {
        if (webView == null) {
            return;
        }
        errorView.setVisibility(failed ? View.VISIBLE : View.GONE);
        webView.setVisibility(failed ? View.INVISIBLE : View.VISIBLE);
    }

    private void retryLoad() {
        if (webView == null) {
            return;
        }
        String current = webView.getUrl();
        if (current != null && current.startsWith("https://")) {
            webView.reload();
        } else {
            webView.loadUrl(START_URL);
        }
    }

    // ------------------------------------------------------------------
    // 뒤로 가기 / 생명주기
    // ------------------------------------------------------------------

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (webView != null && errorView.getVisibility() != View.VISIBLE && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
            webView.resumeTimers();
        }
    }

    @Override
    protected void onPause() {
        // 화면에 보이지 않는 동안에는 사이트의 자동 갱신(타이머)을 멈춰 배터리와 데이터를 아낀다.
        if (webView != null) {
            webView.onPause();
            webView.pauseTimers();
        }
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) {
            webView.saveState(outState);
        }
    }

    @Override
    protected void onDestroy() {
        destroyWebView();
        super.onDestroy();
    }

    private void destroyWebView() {
        if (webView != null) {
            root.removeView(webView);
            webView.destroy();
            webView = null;
        }
    }
}
