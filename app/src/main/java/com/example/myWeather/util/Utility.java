package com.example.myWeather.util;

import android.text.TextUtils;

import com.example.myWeather.db.City;
import com.example.myWeather.db.Province;
import com.example.myWeather.gson.CityLoc;
import com.example.myWeather.gson.Weather;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import okhttp3.Response;

public class Utility {
    /**
     * 解析和处理服务器返回的省级数据
     */
    public static boolean handleProvinceResponse(String response){
        if (!TextUtils.isEmpty(response)){  //如果返回的数据不为空
            try {
                //将所有的省级数据解析出来，并组装成实体类对像
                JSONArray allProvinces = new JSONArray(response);
                for (int i=0;i<allProvinces.length();i++){
                    JSONObject provinceObject = allProvinces.getJSONObject(i);
                    Province province = new Province();
                    province.setProvinceName(provinceObject.getString("name"));
                    province.setProvinceCode(provinceObject.getInt("id"));
                    //将该实体类对象存入数据库
                    province.save();
                }
                return true;//解析成功
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;//解析失败
    }

    /**
     *解析和处理服务器返回的市级数据
     */
    public static boolean handleCityResponse(String response,int provinceId){
        if (!TextUtils.isEmpty(response)){
            try {
                JSONArray allCities = new JSONArray(response);
                for (int i=0;i<allCities.length();i++){
                    JSONObject cityObject = allCities.getJSONObject(i);
                    City city = new City();
                    city.setCityCode(cityObject.getInt("id"));
                    city.setCityName(cityObject.getString("name"));
                    city.setProvinceId(provinceId);  //所属的省级代号
                    city.save();
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }


    /**
     * 解析和处理城市名字和ID
     */
    public static CityLoc handleLocationrResponse(Response response) {
        try {
            return new Gson().fromJson(response.body().string(), CityLoc.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
