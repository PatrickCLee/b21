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
    private WebView webView;                                //*0
    private EditText max;   //上方輸入欄                  *9
    private LocationManager lmgr;                           //*22
    private RequestQueue queue;                         //*30 先在gradle 載入 volley

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this,         //*21 manifest宣告權限,此處要求權限
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) { //*21
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        init();
    }

    private void init(){
        queue = Volley.newRequestQueue(this);                           //*30
        lmgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);        //*23 取得系統服務,不是new,定位不是此app專用,是大家都可用的

        if(!lmgr.isProviderEnabled(LocationManager.GPS_PROVIDER)){          //*28 如果user沒開手機設定的GPS
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS); //頁面跳轉到設定 讓他開
            startActivityForResult(intent,123);
        }

        webView = findViewById(R.id.webView);               //*0
        max = findViewById(R.id.max);                       //*9
        initWebView();                                      //*1
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {   //*29
        super.onActivityResult(requestCode, resultCode, data);
        if (!lmgr.isProviderEnabled(LocationManager.GPS_PROVIDER)){                     //如果沒開,就log沒gps
            Log.v("brad","NO GPS");
        }
    }

    private void initWebView(){
        webView.setWebViewClient(new WebViewClient());//*2 新增此行之前點選畫面上的連結會跳到瀏覽器app,此行之後一直在此;原本webView不是webViewClient,現在希望它可以有瀏覽器特徵
        WebSettings settings = webView.getSettings();               //*4 瀏覽器的setting在瀏覽器身上,不用new
        settings.setJavaScriptEnabled(true);                        //*4 此處才打開JS
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        //settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(true);                      //*5 亂設上列縮放功能

        webView.addJavascriptInterface(new MyJSObject(),"broyo");       //*12 建構出物件

//        webView.loadUrl("https://www.iii.org.tw");        //*1
        webView.loadUrl("file:///android_asset/brad.html");//*7 註解上行新增這行, "file://"叫做通訊協定,第三條/代表根目錄
    }

    @Override
    protected void onStart() {          //*24也同時創出onPause,在onStart時聽需求,onPause時取消
        super.onStart();
        myListener = new MyListener();      //*26
        lmgr.requestLocationUpdates(LocationManager.GPS_PROVIDER,   //紅字為提示需『要求』權限,但上方已做
                0,0,myListener); //第二參數為多少毫秒更新,三為多少公尺更新(二三哪個先到就做),四為GPS聽到後呼叫程式的listener
    }

    private MyListener myListener;      //*25

    private class MyListener implements LocationListener{   //*25

        @Override
        public void onLocationChanged(Location location) {  //位置改變就會被觸發,location參數得到使用者回傳的gps資訊
            double lat = location.getLatitude();
            double lng = location.getLongitude();
            Log.v("brad", lat + ", " + lng );
            Message message = new Message();
            Bundle data = new Bundle();
            data.putString("urname",lat + ", " + lng);
            message.setData(data);
            uiHandler.sendMessage(message);
//            webView.loadUrl(String.format("javascript:moveTo(%f,%f)",lat,lng));  //*35 一旦gps位置有動作,地圖就會更新至目前位置,但現在要玩test2,故註解
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

    public void test2(View view) { //按下test2,要在地圖上移動到上方輸入欄(安卓建的)所輸入的地址   https://developers.google.com/maps/documentation/geocoding/intro
        String url = "https://maps.googleapis.com/maps/api/geocode/json?address=%s&key=AIzaSyCLk8W31pUZyUEwd2z6Wzld99iipFvo85Y";  //*31 將address改為%s,之後讓使用者輸入
        String url2 = String.format(url, max.getText().toString());     //*31 延續上行觀念,帶入使用者在max輸入的資料
        StringRequest request = new StringRequest(
                Request.Method.GET,                 //因為是url帶參數
                url2,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        parseJSON(response);    //拉到下面parseJSON寫
                    }
                },
                null
        );
        queue.add(request);
    }

    private void parseJSON(String json){                    //*32 此處的json就是回傳回的response資料
        try{
            JSONObject root = new JSONObject(json);
            String status = root.getString("status");   //取得status的值
            if(status.equals("OK")){
                JSONArray results = root.getJSONArray("results");       //拿到陣列,陣列中只有一個元素
                JSONObject result = results.getJSONObject(0);           //拿到物件
                JSONObject geometry = result.getJSONObject("geometry");       //一路往內直到抓到經緯度
                JSONObject location = geometry.getJSONObject("location");
                double lat = location.getDouble("lat");
                double lng = location.getDouble("lng");
                Log.v("brad","geocoding => " + lat + ", " + lng);
                webView.loadUrl(String.format("javascript:moveTo(%f,%f)",lat,lng)); //呼叫方法,移動地圖位置到該處
            }else{
                Log.v("brad","status = " + status);
            }
        }catch (Exception e){
            Log.v("brad",e.toString());
        }
    }

    @Override
    protected void onPause() {                                   //*24
        super.onPause();
        lmgr.removeUpdates(myListener);                           //*27
    }

    @Override
    public void onBackPressed() {
        if(webView.canGoBack()){
            webView.goBack();   //尚有webView.goForward()及webView.reload()
        }else{
            super.onBackPressed();
        }
    }                               //*3至此webview都不支援JS

    public void test1(View view){
        String strMax = max.getText().toString();               //*10 抓到輸入欄的值
        Log.v("brad",strMax);
        Log.v("brad",String.format("javascript:test1(%s)", strMax));
        webView.loadUrl(String.format("javascript:test1(%s)", strMax)); //*10
//        webView.loadUrl("javascript:test1()");                *8
    }

    public class MyJSObject{
        @JavascriptInterface        //必須加上Annotation
        public void callFromJS(String urname){                  //*11 按下畫面上的Android按鈕,最上方輸入欄要出現Android按鈕旁輸入欄輸入的文字
            Log.v("brad","OK " + urname);

            Message message = new Message();    //*16       註解14 max.setText(urname), 新增此段
            Bundle data = new Bundle();
            data.putString("urname",urname);
            message.setData(data);
            uiHandler.sendMessage(message);

//            max.setText(urname);                              //*14
        }
    }

    private UIHandler uiHandler = new UIHandler();
    private class UIHandler extends Handler{                    //*15
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);

            String urname = msg.getData().getString("urname");
            //max.setText(urname);                                    //*17
        }
    }

}
