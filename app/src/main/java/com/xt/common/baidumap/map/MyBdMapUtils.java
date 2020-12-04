package com.xt.common.baidumap.map;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.ImageView;
import android.widget.ZoomControls;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.Circle;
import com.baidu.mapapi.map.CircleOptions;
import com.baidu.mapapi.map.GroundOverlay;
import com.baidu.mapapi.map.GroundOverlayOptions;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.Overlay;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.Polyline;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.map.Stroke;
import com.baidu.mapapi.map.TextureMapView;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.model.LatLngBounds;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.baidu.mapapi.search.route.BikingRouteLine;
import com.baidu.mapapi.search.route.BikingRoutePlanOption;
import com.baidu.mapapi.search.route.BikingRouteResult;
import com.baidu.mapapi.search.route.DrivingRouteResult;
import com.baidu.mapapi.search.route.IndoorRouteResult;
import com.baidu.mapapi.search.route.MassTransitRouteResult;
import com.baidu.mapapi.search.route.OnGetRoutePlanResultListener;
import com.baidu.mapapi.search.route.PlanNode;
import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.search.route.WalkingRouteResult;
import com.baidu.mapapi.utils.CoordinateConverter;
import com.baidu.mapsdkplatform.comapi.location.CoordinateType;
import com.blankj.utilcode.util.Utils;
import com.xt.common.baidumap.BikingRouteOverlay;
import com.xt.common.utils.thread.MyThreadUtils;
import com.xt.samplebaidumap.R;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author xt on 2020/7/14 14:36
 * 使用百度地图SDK的工具类
 */
public class MyBdMapUtils {
    public static class BdMapCommonUtils {
        public static final SparseIntArray ZOOM_INFO = new SparseIntArray();
        /**
         * 3~21
         */
        public static final float ZOOM_BAIDU_MAP = 15f;

        static {
            ZOOM_INFO.put(22, 2);
            ZOOM_INFO.put(21, 5);
            ZOOM_INFO.put(20, 10);
            ZOOM_INFO.put(19, 20);
            ZOOM_INFO.put(18, 50);
            ZOOM_INFO.put(17, 100);
            ZOOM_INFO.put(16, 200);
            ZOOM_INFO.put(15, 500);
            ZOOM_INFO.put(14, 1000);
            ZOOM_INFO.put(13, 2000);
            ZOOM_INFO.put(12, 5000);
            ZOOM_INFO.put(11, 10000);
            ZOOM_INFO.put(10, 20000);
            ZOOM_INFO.put(9, 25000);
            ZOOM_INFO.put(8, 50000);
            ZOOM_INFO.put(7, 100000);
            ZOOM_INFO.put(6, 200000);
            ZOOM_INFO.put(5, 500000);
            ZOOM_INFO.put(4, 1000000);
            ZOOM_INFO.put(3, 2000000);
        }

        private static void updateMapStatus(BaiduMap baiduMap, LatLng centerLocation, float zoom) {
            MapStatus.Builder builder = new MapStatus.Builder();
            MapStatusUpdate update = MapStatusUpdateFactory.newMapStatus(builder.target(centerLocation).zoom(zoom).build());
            if (baiduMap.getProjection() != null) {
                baiduMap.animateMapStatus(update);
            } else if (baiduMap.getMapStatus() == null) {
                baiduMap.setMapStatus(update);
            }
        }

        private static void updateMapStatus(BaiduMap baiduMap, LatLng centerLocation) {
            updateMapStatus(baiduMap, centerLocation, ZOOM_BAIDU_MAP);
        }

        public static void refreshMap(BaiduMap baiduMap) {
            LatLng location = baiduMap.getMapStatus().target;
            float zoom = baiduMap.getMapStatus().zoom - 1.0f;
            updateMapStatus(baiduMap, location, zoom);
        }

        public void overlook(BaiduMap baiduMap, float angle) {
            MapStatus mapStatus = new MapStatus.Builder().overlook(angle).build();
            baiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(mapStatus));
        }

        public void animateMapStatus(BaiduMap baiduMap, List<LatLng> points) {
            if (null == points || points.isEmpty()) {
                return;
            }
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (LatLng point : points) {
                builder.include(point);
            }
            MapStatusUpdate msUpdate = MapStatusUpdateFactory.newLatLngBounds(builder.build());
            baiduMap.animateMapStatus(msUpdate);
        }

        /**
         * 校验double数值是否为0
         *
         * @param value
         * @return
         */
        public static boolean isEqualToZero(double value) {
            return Math.abs(value - 0.0) < 0.01;
        }

        /**
         * 经纬度是否为(0,0)点
         *
         * @return
         */
        public static boolean isZeroPoint(double latitude, double longitude) {
            return isEqualToZero(latitude) && isEqualToZero(longitude);
        }

        /**
         * 经纬度是否为(0,0)点
         *
         * @return
         */
        public static boolean isZeroPoint(LatLng latLng) {
            if (latLng == null) {
                return true;
            }
            return isZeroPoint(latLng.latitude, latLng.longitude);
        }

        public static BitmapDescriptor getBitmapDescriptor(int imageResourceId) {
            return BitmapDescriptorFactory.fromResource(imageResourceId);
        }

        public static boolean positionEquals(LatLng latLng0, LatLng latLng1) {
            if ((latLng0 != null) && (latLng1 != null)) {
                return (latLng0.latitude == latLng1.latitude) && (latLng0.longitude == latLng1.longitude);
            }
            return false;
        }

        public static boolean positionEquals(Marker marker0, Marker marker1) {
            if ((marker0 != null) && (marker1 != null)) {
                return positionEquals(marker0.getPosition(), marker1.getPosition());
            }
            return false;
        }

        //一米的像素点
        public static int getPixelsOneMeter(Activity activity, int zoomLevel) {
            DisplayMetrics dm = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
            int densityDpi = dm.densityDpi;
            int scale = BdMapCommonUtils.ZOOM_INFO.get(zoomLevel);
            return (int) ((densityDpi / 2.54) / scale);
        }

        public static void showScaleControl(TextureMapView mapView, boolean isShow) {
            mapView.showScaleControl(isShow);
        }

        public static void showZoomControls(TextureMapView mapView, boolean isShow) {
            mapView.showZoomControls(isShow);
        }

        public static void showBdLogo(TextureMapView mapView, boolean isShow) {
            // 隐藏logo
            View child = mapView.getChildAt(1);
            if ((child instanceof ImageView) || (child instanceof ZoomControls)) {
                if (isShow) {
                    child.setVisibility(View.VISIBLE);
                } else {
                    child.setVisibility(View.INVISIBLE);
                }
            }
        }

        public static void initBdMap(TextureMapView bdMapView) {
            MyBdMapUtils.BdMapCommonUtils.showBdLogo(bdMapView, false);
            MyBdMapUtils.BdMapCommonUtils.showScaleControl(bdMapView, false);
            MyBdMapUtils.BdMapCommonUtils.showZoomControls(bdMapView, false);

            refreshMap(bdMapView.getMap());
        }

        public static void setOnMapClickListener(BaiduMap baiduMap, BaiduMap.OnMapClickListener onMapClickListener) {
            baiduMap.setOnMapClickListener(onMapClickListener);
        }

        public static LatLng getLatLng(BDLocation bdLocation) {
            return new LatLng(bdLocation.getLatitude(), bdLocation.getLongitude());
        }

        public static LatLng getLatLng(String latitude, String longitude) {
            try {
                return new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));
            } catch (Exception e) {
                return null;
            }
        }

        public static LatLng getLatLng(double latitude, double longitude) {
            return new LatLng(latitude, longitude);
        }

        public static Overlay showLocation(BaiduMap baiduMap, LatLng latLng, int resourceId) {
            Overlay overlay = OverlayUtils.addMarkerOverlay(baiduMap, latLng, resourceId);
            updateMapStatus(baiduMap, latLng);
            return overlay;
        }

        public static Overlay showLocation(BaiduMap baiduMap, LatLng latLng) {
            return showLocation(baiduMap, latLng, R.drawable.wsdk_drawable_rg_ic_car3d);
        }

        /**
         * 百度地图坐标转成高德地图坐标，不知道原理
         *
         * @param location
         * @return
         */
        public static LatLng coordinateConvertFromBD09LL(LatLng location) {
            CoordinateConverter coordinateConverter = new CoordinateConverter();
            coordinateConverter.from(CoordinateConverter.CoordType.BD09LL);
            coordinateConverter.coord(location);
            return coordinateConverter.convert();

        }

        /**
         * 将GPS设备采集的原始GPS坐标转换成百度坐标
         *
         * @param location
         * @return
         */
        public static LatLng coordinateConvertFromWGS84(LatLng location) {
            CoordinateConverter coordinateConverter = new CoordinateConverter();
            coordinateConverter.from(CoordinateConverter.CoordType.GPS);
            coordinateConverter.coord(location);
            LatLng convert = coordinateConverter.convert();
            if (convert == null) {
                convert = new LatLng(0, 0);
            }
            return convert;

        }

        /**
         * 将google地图、soso地图、aliyun地图、mapabc地图和amap地图// 所用坐标转换成百度坐标
         *
         * @param location
         * @return
         */
        public static LatLng coordinateConvert(LatLng location) {
            CoordinateConverter coordinateConverter = new CoordinateConverter();
            coordinateConverter.from(CoordinateConverter.CoordType.COMMON);
            coordinateConverter.coord(location);
            LatLng convert = coordinateConverter.convert();
            if (convert == null) {
                convert = new LatLng(0, 0);
            }
            return convert;

        }

        public static void latLng2Address(final LatLng latLng,OnGetGeoCoderResultListener onGetGeoCoderResultListener) {
            MyThreadUtils.doBackgroundWork(new Runnable() {
                @Override
                public void run() {
                    GeoCoder geoCoder = GeoCoder.newInstance();
                    //        XTLogUtil.d("百度经纬度转地址");
                    //设置地址或经纬度反编译后的监听,这里有两个回调方法,
                    geoCoder.setOnGetGeoCodeResultListener(new OnGetGeoCoderResultListener() {
                        //经纬度转换成地址
                        @Override
                        public void onGetReverseGeoCodeResult(ReverseGeoCodeResult result) {
                            if ((result != null && result.error == SearchResult.ERRORNO.NO_ERROR)) {
                                if (onGetGeoCoderResultListener != null) {
                                    onGetGeoCoderResultListener.onGetReverseGeoCodeResult(result);
                                }
                            }
//                XTLogUtil.d("地址:" + addressDetail.province+" "+addressDetail.city+" "+addressDetail.district);
                        }

                        //把地址转换成经纬度
                        @Override
                        public void onGetGeoCodeResult(GeoCodeResult result) {
                            // 详细地址转换在经纬度
                            String address = result.getAddress();
                        }
                    });
                    // 设置反地理经纬度坐标,请求位置时,需要一个经纬度
                    try {
                        geoCoder.reverseGeoCode(new ReverseGeoCodeOption().location(latLng));
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    /**
     * 定位
     */
    public static class MyLocationUtils {
        /**
         * Return whether <em>you</em> have been granted the permissions.
         *
         * @param permissions The permissions.
         * @return {@code true}: yes<br>{@code false}: no
         */
        private static boolean isGranted(final String... permissions) {
            for (String permission : permissions) {
                if (!isGranted(permission)) {
                    return false;
                }
            }
            return true;
        }

        private static boolean isGranted(final String permission) {
            return Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                    || PackageManager.PERMISSION_GRANTED
                    == ContextCompat.checkSelfPermission(Utils.getApp(), permission);
        }

        public static boolean isLocationPermissionGranted() {
            return isGranted(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION);
        }

        @NonNull
        private static LocationClientOption getLocationClientOption() {
            LocationClientOption clientOption = new LocationClientOption();
            // 打开gps
            clientOption.setOpenGps(true);
            //可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
            clientOption.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
            //可选，默认gcj02，设置返回的定位结果坐标系，如果配合百度地图使用，建议设置为bd09ll
            clientOption.setCoorType(CoordinateType.BD09LL);
            //可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
            clientOption.setScanSpan(0);
            //可选，设置是否需要地址信息，默认不需要
            clientOption.setIsNeedAddress(true);
            //可选，设置是否需要地址描述
            clientOption.setIsNeedLocationDescribe(true);
            //可选，设置是否需要设备方向结果
            clientOption.setNeedDeviceDirect(false);
            //可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
            clientOption.setLocationNotify(false);
            //可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
            clientOption.setIgnoreKillProcess(true);
            //可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
            clientOption.setIsNeedLocationDescribe(true);
            //可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
            clientOption.setIsNeedLocationPoiList(true);
            //可选，默认false，设置是否收集CRASH信息，默认收集
            clientOption.SetIgnoreCacheException(false);
            //可选，默认false，设置定位时是否需要海拔信息，默认不需要，除基础定位版本都可用
            clientOption.setIsNeedAltitude(false);
            return clientOption;
        }

        /**
         * 定位之前要获取位置权限
         *
         * @return
         */
        public static void startLocation(BDAbstractLocationListener bdAbstractLocationListener) {
            final LocationClient client = new LocationClient(Utils.getApp());
            client.setLocOption(getLocationClientOption());
            client.registerLocationListener(new BDAbstractLocationListener() {
                @Override
                public void onReceiveLocation(BDLocation bdLocation) {
                    client.stop();
                    client.unRegisterLocationListener(this);
                    /*double latitude  = bdLocation.getLatitude();
                    double longitude = bdLocation.getLongitude();
                    if (!BdMapCommonUtils.isZeroPoint(latitude, longitude)) {

                    }*/
                    if (bdAbstractLocationListener != null) {
                        bdAbstractLocationListener.onReceiveLocation(bdLocation);
                    }
                }
            });
            MyThreadUtils.doBackgroundWork(new Runnable() {
                @Override
                public void run() {
                    client.start();
                }
            });
        }

        public static Overlay showMyLocation(BaiduMap baiduMap, BDLocation bdLocation) {
            LatLng latLng = MyBdMapUtils.BdMapCommonUtils.getLatLng(bdLocation);
            return BdMapCommonUtils.showLocation(baiduMap, latLng);
        }
    }

    /**
     * 可以移动的覆盖物
     */
    public static class MoveOverlayUtils {
        /**
         * 算斜率
         */
        public static double getSlope(LatLng fromPoint, LatLng toPoint) {
            if (toPoint.longitude == fromPoint.longitude) {
                return Double.MAX_VALUE;
            }
            return (toPoint.latitude - fromPoint.latitude) / (toPoint.longitude - fromPoint.longitude);
        }

        /**
         * 根据两点算取图标转的角度
         */
        public static double getAngle(LatLng fromPoint, LatLng toPoint) {
            double slope = getSlope(fromPoint, toPoint);
            if (slope == Double.MAX_VALUE) {
                if (toPoint.latitude > fromPoint.latitude) {
                    return 0;
                } else {
                    return 180;
                }
            }
            float deltAngle = 0;
            if ((toPoint.latitude - fromPoint.latitude) * slope < 0) {
                deltAngle = 180;
            }
            double radio = Math.atan(slope);
            return 180 * (radio / Math.PI) + deltAngle - 90;
        }

        /**
         * 根据点和斜率算取截距
         */
        public static double getInterception(double slope, LatLng startPoint) {
            return startPoint.latitude - slope * startPoint.longitude;
        }

        /**
         * 计算x方向每次移动的距离
         */
        public static double getXMoveDistance(double slope) {
            double DISTANCE = 0.0001;
            if (slope == Double.MAX_VALUE) {
                return DISTANCE;
            }
            return Math.abs((DISTANCE * slope) / Math.sqrt(1 + slope * slope));
        }

        /**
         * 通过递归调用实现轨迹回放
         *
         * @param moveMarker
         * @param startPoint
         * @param endPoint
         */
        public static void setMoveMarkPosition(final Marker moveMarker, LatLng startPoint, LatLng endPoint, Long millis) {
            if (moveMarker == null) {
                return;
            }
            if (startPoint == null) {
                return;
            }
            if (endPoint == null) {
                return;
            }
            if (millis == null) {
                millis = 5L;
            }

            moveMarker.setPosition(startPoint);
            moveMarker.setRotate((float) getAngle(startPoint, endPoint));
            //是不是正向的标示
            boolean isReverse = (startPoint.latitude > endPoint.latitude);
            final double slope = getSlope(startPoint, endPoint);
            double interception = getInterception(slope, startPoint);
            double xMoveDistance = getXMoveDistance(slope);
            xMoveDistance = isReverse ? xMoveDistance : (-1 * xMoveDistance);
            for (double j = startPoint.latitude; ((j > endPoint.latitude) == isReverse); j = j - xMoveDistance) {
                LatLng latLng;
                if (slope == Double.MAX_VALUE) {
                    latLng = new LatLng(j, startPoint.longitude);
                } else {
                    latLng = new LatLng(j, (j - interception) / slope);
                }
                moveMarker.setPosition(latLng);
                try {
                    Thread.sleep(millis);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public static Marker addMoveMarker(BaiduMap baiduMap, Marker moveMarker, LatLng current, LatLng next) {
            if (moveMarker != null) {
                moveMarker.remove();
            }
            BitmapDescriptor myLocationIcon = BitmapDescriptorFactory
                    .fromResource(R.drawable.wsdk_icon_classic);
            OverlayOptions markerOptions =
                    new MarkerOptions()
                            .flat(true)
                            .anchor(0.5f, 0.5f)
                            .icon(myLocationIcon)
                            .position(current)
                            .rotate((float) getAngle(current, next));
            moveMarker = (Marker) baiduMap.addOverlay(markerOptions);
            moveMarker.setRotate((float) getAngle(current, next));
            return moveMarker;
        }
    }

    public static class TrackPlayer {
        private Handler handler = MyThreadUtils.getThreadHandler();

        private Marker moveMarker;
        private List<LatLng> latLngList;
        private int index = 0;

        /**
         * 回放倍范围:  1-10  [1最慢, 10最快,默认5]
         */
        private final Map<Integer, Long> playbackRateMap = new HashMap<>();
        private long millis = 10;

        private boolean isPlaying = false;

        private PlayStateListener playStateListener;

        public TrackPlayer(Marker moveMarker, List<LatLng> latLngList, int playbackRate, PlayStateListener playStateListener) {
            this.moveMarker = moveMarker;
            this.latLngList = latLngList;

            for (int i = 1; i < 11; i++) {
                playbackRateMap.put(i, 55L - 5 * i);
            }
            setPlaybackRate(playbackRate);

            this.playStateListener = playStateListener;
        }

        public void clickPlay() {
            if (isPlaying) {

            } else {
                if (isPlayFinish()) {
                    index = 0;
                    if (isPlayFinish()) {
                        return;
                    }
                    if (playStateListener != null) {
                        playStateListener.onPlayState(true);
                    }
                    playTrack();
                }
            }
        }

        private void playTrack() {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (latLngList != null) {
                        if (isPlayFinish()) {
                            if (playStateListener != null) {
                                playStateListener.onPlayState(false);
                            }
                        } else {
                            MoveOverlayUtils.setMoveMarkPosition(moveMarker, latLngList.get(index), latLngList.get(index + 1), millis);
                            index = +1;
                            if (isPlaying) {
                                playTrack();
                            }
                        }
                    }
                }
            });
        }

        private boolean isPlayFinish() {
            return latLngList.size() > index + 1;
        }

        public void setPlaybackRate(int playbackRate) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (playbackRate > 0 && playbackRate < 11) {
                        millis = playbackRateMap.get(playbackRate);
                    }
                }
            });
        }

        public static interface PlayStateListener {
            void onPlayState(boolean isPlaying);
        }
    }

    public static class OverlayUtils {
        public static Circle addCircleOverlay(BaiduMap baiduMap, LatLng center, int radius) {
            CircleOptions circleOptions = new CircleOptions().fillColor(0x4C5C9EFF);
            circleOptions.stroke(new Stroke(2, 0xFF5C9EFF))
                    .center(center)
                    .radius(radius);
            return (Circle) baiduMap.addOverlay(circleOptions);
        }

        public static GroundOverlay addGroundOverlay(BaiduMap baiduMap, LatLng latLng, BitmapDescriptor icon, Bundle bundle, int width, int height) {
            OverlayOptions overlayOptions = new GroundOverlayOptions().position(latLng)
                    .image(icon).zIndex(9).dimensions(width, height);
            GroundOverlay groundOverlay = (GroundOverlay) baiduMap.addOverlay(overlayOptions);
            if (null != bundle) {
                groundOverlay.setExtraInfo(bundle);
            }
            return groundOverlay;
        }

        public static Marker addMarkerOverlay(BaiduMap baiduMap, LatLng position, BitmapDescriptor icon, Bundle bundle) {
            OverlayOptions overlayOptions = new MarkerOptions().position(position)
                    .icon(icon).zIndex(9).animateType(MarkerOptions.MarkerAnimateType.none).draggable(true).perspective(true).flat(false);
            Marker marker = (Marker) baiduMap.addOverlay(overlayOptions);
            if (null != bundle) {
                marker.setExtraInfo(bundle);
            }
            return marker;
        }

        public static Marker addMarkerOverlay(BaiduMap baiduMap, LatLng currentPoint, int resourceId) {
            BitmapDescriptor myLocationIcon = BitmapDescriptorFactory
                    .fromResource(resourceId);
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(currentPoint)
                    .icon(myLocationIcon);
            return (Marker) baiduMap.addOverlay(markerOptions);
        }

        /**
         * 在地图上绘制折线
         *
         * @param mapView
         * @param points
         * @param color
         * @return
         */
        public static Polyline addPolylineOverlay(TextureMapView mapView, List<LatLng> points, int color) {
            BaiduMap baiduMap = mapView.getMap();
            baiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
            baiduMap.setBuildingsEnabled(false);

            // 添加路线（轨迹）
            OverlayOptions polylineOptions = new PolylineOptions().width(10)
                    .color(color).points(points);
            //路线覆盖物
            return (Polyline) baiduMap.addOverlay(polylineOptions);
        }
    }

    public static class PathPlanningUtils {

        private BikingRouteOverlay bikingRouteOverlay;

        public void pathPlanning(BaiduMap baiduMap, LatLng start, LatLng stop, long delay) {
            RoutePlanSearch routePlanSearch = RoutePlanSearch.newInstance();
            routePlanSearch.setOnGetRoutePlanResultListener(new OnGetRoutePlanResultListener() {
                @Override
                public void onGetWalkingRouteResult(WalkingRouteResult walkingRouteResult) {

                }

                @Override
                public void onGetTransitRouteResult(TransitRouteResult transitRouteResult) {

                }

                @Override
                public void onGetMassTransitRouteResult(MassTransitRouteResult massTransitRouteResult) {

                }

                @Override
                public void onGetDrivingRouteResult(DrivingRouteResult drivingRouteResult) {

                }

                @Override
                public void onGetIndoorRouteResult(IndoorRouteResult indoorRouteResult) {

                }

                @Override
                public void onGetBikingRouteResult(BikingRouteResult bikingRouteResult) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {

                            {
                                if (bikingRouteResult == null) {
//                                R.string.pathPlanError3
                                    return;
                                }
                                if (bikingRouteResult.error != SearchResult.ERRORNO.NO_ERROR) {
                                    if (bikingRouteResult.error == SearchResult.ERRORNO.RESULT_NOT_FOUND) {
//                                    R.string.pathPlanError0
                                        return;
                                    }
                                    if (bikingRouteResult.error == SearchResult.ERRORNO.ST_EN_TOO_NEAR) {
//                                    R.string.pathPlanError1
                                        return;
                                    }
                                    if (bikingRouteResult.error == SearchResult.ERRORNO.NETWORK_ERROR) {
//                                    R.string.pathPlanError2
                                        return;
                                    }
//                                R.string.pathPlanError3
                                    return;
                                }

                                List<BikingRouteLine> routeLines = bikingRouteResult.getRouteLines();
                                if (routeLines == null || routeLines.get(0) == null || routeLines.get(0).getStarting() == null || routeLines.get(0).getTerminal() == null) {
//                                R.string.pathPlanError3
                                    return;
                                }
                            }

                            {
                                if (bikingRouteOverlay != null) {
                                    bikingRouteOverlay.removeFromMap();
                                    bikingRouteOverlay = null;
                                }

                                bikingRouteOverlay = new MyBikingRouteOverlay(baiduMap);
                                List<BikingRouteLine> routeLines = bikingRouteResult.getRouteLines();
                                bikingRouteOverlay.setData(routeLines.get(0));
                                bikingRouteOverlay.addToMap();
                                bikingRouteOverlay.zoomToSpan();
                            }
                        }
                    }, delay);
                }
            });
            PlanNode stNode = PlanNode.withLocation(start);
            PlanNode enNode = PlanNode.withLocation(stop);
            routePlanSearch.bikingSearch((new BikingRoutePlanOption())
                    .from(stNode)
                    .to(enNode));
        }
    }

    private static class MyBikingRouteOverlay extends BikingRouteOverlay {
        public MyBikingRouteOverlay(BaiduMap baiduMap) {
            super(baiduMap);
        }

        @Override
        public BitmapDescriptor getStartMarker() {
            return BitmapDescriptorFactory.fromResource(R.drawable.bn_start_blue);
        }

        @Override
        public BitmapDescriptor getTerminalMarker() {
            return BitmapDescriptorFactory.fromResource(R.drawable.bn_dest_blue);
        }

        @Override
        protected void onStartClick() {
            super.onStartClick();
        }

        @Override
        protected void onEndClick() {
            super.onStartClick();
        }
    }
}
