package tw.org.iii.brad.brad21;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private EditText max;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        max = findViewById(R.id.max);
        initWebView();
    }

    private void initWebView(){
        webView.setWebViewClient(new WebViewClient());//新增此行之前點選畫面上的連結會跳到瀏覽器app,此行之後一直在此;原本webView不是webViewClient,現在希望它可以有瀏覽器特徵
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);    //此處才打開JS
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(true);

//        webView.loadUrl("https://www.iii.org.tw");
        webView.loadUrl("file:///android_asset/brad.html");//註解上行新增這行, "file://"叫做通訊協定,第三條/代表根目錄
    }

    @Override
    public void onBackPressed() {
        if(webView.canGoBack()){
            webView.goBack();   //尚有webView.goForward()及webView.reload()
        }else{
            super.onBackPressed();
        }
    }                               //3結束之前此webview都不支援JS

    public void test1(View view){
        String strMax = max.getText().toString();
        Log.v("brad",strMax);
        Log.v("brad",String.format("javascript:test1(%s)", strMax));
        webView.loadUrl(String.format("javascript:test1(%s)", strMax)); //此處是從loadUrl裡面的頁面去呼叫此方法,見5
//        webView.loadUrl("javascript:test1()");
    }
}
