package tw.org.iii.brad.brad21;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private EditText max;   //上方輸入欄
    private LocationManager lmgr;
    private RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // No explanation needed; request the permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    123);

        } else {
            init();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        init();
    }

    private void init(){
        queue = Volley.newRequestQueue(this);
        lmgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if(!lmgr.isProviderEnabled(LocationManager.GPS_PROVIDER)){  //如果它沒開GPS
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS); //頁面跳轉到設定 讓它開
            startActivityForResult(intent,123);
        }

        webView = findViewById(R.id.webView);
        max = findViewById(R.id.max);
        initWebView();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (!lmgr.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            Log.v("brad","NO GPS");
        }
    }

    private void initWebView(){
        webView.setWebViewClient(new WebViewClient());//新增此行之前點選畫面上的連結會跳到瀏覽器app,此行之後一直在此;原本webView不是webViewClient,現在希望它可以有瀏覽器特徵
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);    //此處才打開JS
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        //settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(true);

        webView.addJavascriptInterface(new MyJSObject(),"broyo");

//        webView.loadUrl("https://www.iii.org.tw");
        webView.loadUrl("file:///android_asset/brad.html");//註解上行新增這行, "file://"叫做通訊協定,第三條/代表根目錄
    }

    @Override
    protected void onStart() {  //也同時創出onPause,在onStart時聽需求,onPause時取消
        super.onStart();
        myListener = new MyListener();
        lmgr.requestLocationUpdates(LocationManager.GPS_PROVIDER,   //紅字為提示需『要求』權限,但CD已做
                0,0,myListener); //第二參數為多少毫秒更新,三為多少公尺更新(哪個先到就做),四為GPS聽到後呼叫程式的listener
    }

    private MyListener myListener;

    public void test2(View view) { //https://developers.google.com/maps/documentation/geocoding/intro
        String url = "https://maps.googleapis.com/maps/api/geocode/json?address=%s&key=AIzaSyCLk8W31pUZyUEwd2z6Wzld99iipFvo85Y";
        String url2 = String.format(url, max.getText().toString());
        StringRequest request = new StringRequest(
                Request.Method.GET,
                url2,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        parseJSON(response);    //拉到下面parsJSON寫
                    }
                },
                null
        );
        queue.add(request);
    }

    private void parseJSON(String json){
        try{
            JSONObject root = new JSONObject(json);
            String status = root.getString("status");
            if(status.equals("OK")){
                JSONArray results = root.getJSONArray("results");
                JSONObject result = results.getJSONObject(0);
                JSONObject geometry = result.getJSONObject("geometry");
                JSONObject location = geometry.getJSONObject("location");
                double lat = location.getDouble("lat");
                double lng = location.getDouble("lng");
                Log.v("brad","geocoding => " + lat + ", " + lng);
                webView.loadUrl(String.format("javascript:moveTo(%f,%f)",lat,lng));
            }else{
                Log.v("brad","status = " + status);
            }
        }catch (Exception e){
            Log.v("brad",e.toString());
        }
    }

    private class MyListener implements LocationListener{

        @Override
        public void onLocationChanged(Location location) {
            double lat = location.getLatitude();
            double lng = location.getLongitude();
            Log.v("brad", lat + ", " + lng );
            Message message = new Message();    //註解下方max.setText(urname), 新增此段
            Bundle data = new Bundle();
            data.putString("urname",lat + ", " + lng);
            message.setData(data);
            uiHandler.sendMessage(message);
            webView.loadUrl(String.format("javascript:moveTo(%f,%f)",lat,lng));
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        lmgr.removeUpdates(myListener);
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

    public class MyJSObject{
        @JavascriptInterface        //必須加上Annotation
        public void callFromJS(String urname){
            Log.v("brad","OK " + urname);

            Message message = new Message();    //註解下方max.setText(urname), 新增此段
            Bundle data = new Bundle();
            data.putString("urname",urname);
            message.setData(data);
            uiHandler.sendMessage(message);

//            max.setText(urname);
        }
    }

    private UIHandler uiHandler = new UIHandler();
    private class UIHandler extends Handler{
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);

            String urname = msg.getData().getString("urname");
            max.setText(urname);
        }
    }

}
