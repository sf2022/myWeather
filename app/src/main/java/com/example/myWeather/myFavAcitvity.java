package com.example.myWeather;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myWeather.db.City;
import com.example.myWeather.db.realWeather;

import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.List;

public class myFavAcitvity extends AppCompatActivity implements View.OnClickListener{
    private ListView favList_lv;
    private Button back_bt;
    private List<realWeather> rwList;//市列表
    private ArrayAdapter<String> adapter;
    private List<String> cityNameList=new ArrayList<>();
    private List<String> locList=new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_myfav);
        initView();

        queryMyList();
        cancelMyFav();
    }

    /**
     * 查询关注城市列表
     */
    private void queryMyList() {
        if(rwList!=null){
            rwList.clear();
        }
        if (cityNameList!=null)
            cityNameList.clear();
        rwList = DataSupport.where("fav>?","0").find(realWeather.class);
        for (realWeather rw:rwList){ //提取关注的城市名称和location
            cityNameList.add(rw.getCurCity());
            locList.add(rw.getLocation());
        }
        adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,cityNameList);
        favList_lv.setAdapter(adapter);
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:
                finish();
        }
    }

    /**
     * 取消关注
     */
    public void cancelMyFav(){
        favList_lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final
            int position, long id) {
                AlertDialog dialog;
                AlertDialog.Builder builder = new AlertDialog.Builder(myFavAcitvity.this)
                    .setMessage("是否删除此记录")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            realWeather rw = rwList.get(position);
                            rw.setFav(false);
                            rw.save();
                            //adapter.notifyDataSetChanged();//刷新界面
                            queryMyList();
                            Toast.makeText(myFavAcitvity.this, "取消关注",
                                    Toast.LENGTH_LONG).show();

                        }
                    })
                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();//取消对话框
                        }
                    });
                dialog = builder.create();
                dialog.show();
                return true;
            }
        });
    }
    public void initView(){
        favList_lv=(ListView)findViewById(R.id.favList);
        back_bt=(Button) findViewById(R.id.back);
        back_bt.setOnClickListener(this);
        favList_lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String curCity = cityNameList.get(position);
                String location= locList.get(position);
                Intent intent = new Intent(myFavAcitvity.this, WeatherActivity.class);

                intent.putExtra("curCity",curCity);
                intent.putExtra("location",location);
                startActivityForResult(intent,1);
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            //刷新listview
            queryMyList();
        }
    }
}
