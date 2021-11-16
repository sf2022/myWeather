package com.example.myWeather;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myWeather.gson.Weather;
import com.example.myWeather.db.realWeather;
import com.google.gson.Gson;

import org.litepal.crud.DataSupport;

import java.util.Date;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity implements View.OnClickListener{

    private TextView curCity_tv;
    private TextView weatherInfo_tv;
    private TextView updateTime_tv;
    private TextView humidity_tv;
    private TextView temperature_tv;
    private Button fav_bt;
    private Button refresh_bt;
    private Button back_bt;

    private Weather weather=new Weather();
    private realWeather rWeather=new realWeather();
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);
        initView();
        requestWeather(rWeather);

    }
    /**
     * 天气查询，优先查询数据库，如果没有再去服务器查询
     */
    public void requestWeather(realWeather rWeather){
        String location=rWeather.getLocation();
        String weatherUrl = "https://devapi.qweather.com/v7/weather/now?location="+
                location+"&key=02e04d2c2a4a4deeaefd7fc0c6fa8951";
        List<realWeather> rweathers=DataSupport.where("location=?",location).find(realWeather.class);
        if(rweathers.size()>0){
            rWeather=rweathers.get(0);
            showWeather(rWeather);
        }
        else{
            sendRequestWithOkHttp(weatherUrl,rWeather);
        }
    }
    /**
     * 访问天气API，发送查询天气请求
     */
    private void sendRequestWithOkHttp(String weatherUrl,realWeather rWeather){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    OkHttpClient client=new OkHttpClient();
                    Request request=new Request.Builder().url(weatherUrl).build();
                    Response response=client.newCall((request)).execute();
                    weather=parseJSONWIthJSONbject(response);
                    rWeather.setUpdateTime(weather.getUpdateTime());
                    rWeather.setWeatherInfo(weather.getNow().getText());
                    rWeather.setTemperature(weather.getNow().getTemp());
                    rWeather.setHumidity(weather.getNow().getHumidity());
                    rWeather.save();
                    showWeather(rWeather);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }
    /**
     * 使用JSON解析数据
     */
    private Weather parseJSONWIthJSONbject(Response response){
        try{
            Gson gson=new Gson();
            return gson.fromJson(response.body().string(), Weather.class);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 展示天气信息在界面上
     */
    public void showWeather(realWeather rWeather) {
        runOnUiThread(() -> {
            if(DataSupport.where("location=?",rWeather.getLocation()).
                    find(realWeather.class).get(0).isFav()){
                fav_bt.setText("已关注");
            }
            curCity_tv.setText("城市:" + rWeather.getCurCity());
            weatherInfo_tv.setText("天气状况:" + rWeather.getWeatherInfo());
            updateTime_tv.setText("最近更新时间:" + rWeather.getUpdateTime());
            humidity_tv.setText("湿度:" + rWeather.getHumidity());
            temperature_tv.setText("温度:" + rWeather.getTemperature());

        });
    }
    //初始化控件
    public void initView(){
        curCity_tv=(TextView) findViewById(R.id.curCity);
        weatherInfo_tv=(TextView) findViewById(R.id.weatherInfo);
        updateTime_tv=(TextView) findViewById(R.id.updateTime);
        humidity_tv=(TextView) findViewById(R.id.humidity);
        temperature_tv=(TextView) findViewById(R.id.temperature);
        fav_bt=(Button)findViewById(R.id.fav);
        refresh_bt=(Button)findViewById(R.id.refresh);
        back_bt=(Button)findViewById(R.id.back);
        Intent intent = getIntent();
        if(intent!=null){
            rWeather.setLocation(intent.getStringExtra("location"));
            rWeather.setCurCity(intent.getStringExtra("curCity"));
        }

        fav_bt.setOnClickListener(this);
        refresh_bt.setOnClickListener(this);
        back_bt.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.refresh:
                String location=rWeather.getLocation();
                String weatherUrl = "https://devapi.qweather.com/v7/weather/now?location="+
                        location+"&key=02e04d2c2a4a4deeaefd7fc0c6fa8951";
                sendRequestWithOkHttp(weatherUrl,rWeather);
                Toast.makeText(this,"已刷新",Toast.LENGTH_LONG).show();
                break;
            case R.id.fav:
                List<realWeather> rws= DataSupport.where("location=?",
                        rWeather.getLocation()).find(realWeather.class);
                rWeather=rws.get(0);
                if(rWeather.isFav()){
                    rWeather.setFav(false);
                    fav_bt.setText("关注");
                }
                else {
                    rWeather.setFav(true);
                    fav_bt.setText("已关注");
                }
                rWeather.save();
                break;
            case R.id.back:
                setResult(RESULT_OK);
                finish();
            default:
                break;
        }
    }
}