package com.putao.ptx.camera.activity;

import android.util.Log;

import com.android.camera.exif.ExifInterface;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.service.LocationService;
import com.putao.ptx.camera.PTUIApplication;
import com.sunnybear.library.controller.BasicFragmentActivity;

/***
 * 单点定位示例，用来展示基本的定位结果，配置在LocationService.java中
 * 默认配置也可以在LocationService中修改
 * 默认配置的内容自于开发者论坛中对开发者长期提出的疑问内容
 *
 * @author baidu
 */
public abstract class LocationActivity extends BasicFragmentActivity {
    private LocationService locationService;

    /***
     * Stop location service
     */
    @Override
    protected void onStop() {
        // TODO Auto-generated method stub
        locationService.stop(); //停止定位服务
        super.onStop();
    }

    @Override
    protected void onStart() {
        // TODO Auto-generated method stub
        super.onStart();
        // -----------location config ------------
        locationService = ((PTUIApplication) getApplication()).locationService;
        //获取locationservice实例，建议应用中只初始化1个location实例，然后使用，可以参考其他示例的activity，都是通过此种方式获取locationservice实例的

        //注册监听
        int type = getIntent().getIntExtra("from", 0);
        if (type == 0) {
            locationService.setLocationOption(locationService.getDefaultLocationClientOption());
        } else if (type == 1) {
            locationService.setLocationOption(locationService.getOption());
        }
        locationService.start();
    }


    private static final String TAG = "LocationActivity";

    public BDLocation getCurBdLocation() {
        return locationService.getCurBdLocation();
    }

    @Override
    protected void onResume() {
        super.onResume();

        LocationClient locationClient = locationService.getLocationClient();
        boolean isStarted = locationClient.isStarted();
        Log.d(TAG, "onResume: isStartedLocationService:" + isStarted);
    }

    public String getAddress() {
        return locationService.getAddress();
    }

    public ExifInterface getExif() {
        return locationService.getExif();
    }
}
