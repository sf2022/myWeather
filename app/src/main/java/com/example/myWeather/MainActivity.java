package com.example.myWeather;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.example.myWeather.db.City;
import com.example.myWeather.db.Province;
import com.example.myWeather.gson.CityLoc;
import com.example.myWeather.util.Utility;
import com.google.gson.Gson;

import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    public static final int LEVEL_PROVINCE=0;
    public static final int LEVEL_CITY=1;
    private int currentLevel;  //当前被选中的级别
    private Province selectedProvince;//被选中的省份
    private City selectedCity;//被选中的城市
    private ArrayAdapter<String> adapter;
    private List<String> dataList = new ArrayList<>();
    private List<Province> provinceList;//省列表
    private List<City> cityList;//市列表
    String curCity;
    String location;
    EditText search_tv;
    ListView list_view_lv;
    Button confirm_bt;
    Button myFav_bt;
    Button back_bt;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,dataList);
        list_view_lv.setAdapter(adapter);
        queryProvinces();
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.confirm:
                String location=search_tv.getText().toString();
                String CityrUrl = "https://geoapi.qweather.com/v2/city/lookup?location="+
                        location+"&key=02e04d2c2a4a4deeaefd7fc0c6fa8951";
                City selectedCity=new City();
                selectedCity.setLocation(location);
                LocqueryFromServer(CityrUrl,selectedCity);
                break;
            case R.id.myFav:
                Intent intent2 = new Intent(MainActivity.this, myFavAcitvity.class);
                startActivity(intent2);
            case R.id.back:
                if (currentLevel == LEVEL_CITY){
                    queryProvinces();
                }
            default:
                break;
        }
    }

    /**
     * 查询城市的location
     */
    public void requestLocation(String curCity){

        String CityrUrl = "https://geoapi.qweather.com/v2/city/lookup?location="+
                curCity+"&key=02e04d2c2a4a4deeaefd7fc0c6fa8951";
        List<City> cities=DataSupport.where("cityName=?",curCity).find(City.class);
        if(cities.size()>0){
            City c=cities.get(0);
            //先从数据库中查
            if (c.getLocation()!=null){
                location=c.getLocation();
                toNextAct(c);
            }
            //数据库没有，再去服务器查
            else
                LocqueryFromServer(CityrUrl,selectedCity);
        }
    }

    /**
     * 发送查询请求--查询城市location
     */
    private void LocqueryFromServer(String CityUrl,City selectedCity){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder().url(CityUrl).build();
                    Response response = client.newCall((request)).execute();

                    CityLoc cl = Utility.handleLocationrResponse(response);
                    if(!cl.getCode().equals("200")){
                        showToast("无此城市");
                    }
                    else {
                        location=cl.getLocation().get(0).getId();
                        curCity=cl.getLocation().get(0).getName();
                        selectedCity.setCityName(curCity);
                        selectedCity.setLocation(location);
                        selectedCity.save();
                        toNextAct(selectedCity);
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 跳转天气显示页面
     * @param selectedCity
     */
    private void toNextAct(City selectedCity) {
        runOnUiThread(()->{
            Intent intent = new Intent(MainActivity.this, WeatherActivity.class);
            intent.putExtra("curCity",selectedCity.getCityName());
            intent.putExtra("location",selectedCity.getLocation());
            startActivity(intent);
        });
    }

    private void showToast(String message){
        runOnUiThread(()->{
            Toast.makeText(MainActivity.this,message,Toast.LENGTH_SHORT).show();
        });
    }
    /**
     * 根据传入的地址和类型从服务器查询省和市数据
     */
    public void queryFromServer(String adress, final String type) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    OkHttpClient client=new OkHttpClient();
                    Request request=new Request.Builder().url(adress).build();
                    Response response=client.newCall((request)).execute();
                    String responData=response.body().string();
                    boolean result = false;

                    if ("province".equals(type)){
                        result=Utility.handleProvinceResponse(responData);
                    }
                    else if ("city".equals(type)){
                        result= Utility.handleCityResponse(responData,selectedProvince.getId());
                    }
                    if(result){
                        if ("province".equals(type)){
                            queryProvinces();
                        }
                        else if ("city".equals(type)){
                            queryCities();
                        }
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 查询该省所有的市，优先查询数据库，如果没有再去服务器查询
     */
    public void queryCities() {
        runOnUiThread(() -> {
            //查询被选中的省份城市的市区
            cityList = DataSupport.where("provinceid=?", String.valueOf(selectedProvince.
                    getId())).find(City.class);
            //如果省列表不为空，则...
            if (cityList.size() > 0) {
                dataList.clear();
                for (City city : cityList) { //遍历每一份省的市级城市
                    dataList.add(city.getCityName()); //添加到数据列表中
                }
                adapter.notifyDataSetChanged();//通知适配器数据更新了
                list_view_lv.setSelection(0);
                currentLevel = LEVEL_CITY;
            }
            else {
                //获取被选取省级代码
                int provinceCode = selectedProvince.getProvinceCode();
                //获取被选取地区的网络地址
                String address = "http://guolin.tech/api/china/" + provinceCode;
                //Log.d("ChooseAreaFragment","准备在网络中获取地址信息");
                queryFromServer(address, "city");   // 在网络中查询
            }
        });
    }
    /**
     *全国所有的省，优先查询数据库，如果没有再去服务器查询
     */
    public void queryProvinces() {
        runOnUiThread(() -> {
            provinceList = DataSupport.findAll(Province.class);
            if (provinceList.size() > 0) {
                dataList.clear();
                for (Province province : provinceList) {
                    dataList.add(province.getProvinceName());
                }
                adapter.notifyDataSetChanged();
                list_view_lv.setSelection(0);
                currentLevel = LEVEL_PROVINCE;
            }
            else {
                //Log.d("ChooseAreaFragment","服务器查询省中...");
                String address = "http://guolin.tech/api/china";
                queryFromServer(address, "province");
            }
        });
    }

    public void initView(){
        search_tv=(EditText)findViewById(R.id.search);
        list_view_lv=(ListView)findViewById(R.id.list_view);
        confirm_bt=(Button) findViewById(R.id.confirm);
        myFav_bt=(Button)findViewById(R.id.myFav);
        back_bt=(Button)findViewById(R.id.back);
        confirm_bt.setOnClickListener(this);
        myFav_bt.setOnClickListener(this);
        back_bt.setOnClickListener(this);
        list_view_lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel == LEVEL_PROVINCE){   //当前选中的级别为省份时
                    selectedProvince = provinceList.get(position);  //当前点击为选中状态
                    queryCities();//查询市的方法
                }
                //以下实现地区天气界面
                else if (currentLevel == LEVEL_CITY){
                    selectedCity=cityList.get(position);
                    String curCity=selectedCity.getCityName();
                    requestLocation(curCity);
                }
            }
        });
    }
}
