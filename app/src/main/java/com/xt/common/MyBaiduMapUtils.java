package com.xt.common;

import android.app.Activity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.TextView;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.Overlay;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.model.LatLng;
import com.xt.samplebaidumap.R;

import java.util.List;

/**
 * @author xt on 2020/7/14 14:36
 */
public class MyBaiduMapUtils {
    public static final SparseIntArray ZOOM_INFO      = new SparseIntArray();
    /**
     * 3~21
     */
    public static final float          ZOOM_BAIDU_MAP = 15f;

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

    public static class TrackPlaybackUtils {
        /**
         * 轨迹回放
         *
         * @param moveMarker
         * @param startPoint
         * @param endPoint
         * @return
         */
        public static boolean setMoveMarkPosition(final Marker moveMarker, final LatLng startPoint, final LatLng endPoint) {
            if (moveMarker == null) {
                return false;
            }
            moveMarker.setPosition(startPoint);
            moveMarker.setRotate((float) getAngle(startPoint, endPoint));
            //是不是正向的标示
            boolean      isReverse     = (startPoint.latitude > endPoint.latitude);
            final double slope         = getSlope(startPoint, endPoint);
            double       interception  = getInterception(slope, startPoint);
            double       xMoveDistance = getXMoveDistance(slope);
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
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return true;
        }
    }

    public Marker addMoveMarker(BaiduMap baiduMap, Marker moveMarker, LatLng current, LatLng next) {
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

    public void addMarker(BaiduMap baiduMap, LatLng currentPoint, int resourceId) {
        BitmapDescriptor myLocationIcon = BitmapDescriptorFactory
                .fromResource(resourceId);
        MarkerOptions markerOptions = new MarkerOptions()
                .position(currentPoint)
                .icon(myLocationIcon);
        baiduMap.addOverlay(markerOptions);
    }

    //一米的像素点
    public static int getPixelsOneMeter(Activity activity, int zoomLevel) {
        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        int densityDpi = dm.densityDpi;
        int scale      = ZOOM_INFO.get(zoomLevel);
        return (int) ((densityDpi / 2.54) / scale);
    }

    public static class TrackUtils {
        /**
         * 绘制历史轨迹
         *
         * @param mapView
         * @param points
         * @param index
         * @param color
         * @param ascend  升序或降序
         * @param endText
         * @return
         */
        public Marker[] drawHistoryTrack(MapView mapView, List<LatLng> points, int index, int color, boolean ascend, String endText) {
            BaiduMap baiduMap = mapView.getMap();
            Marker[] markers  = new Marker[2];
            baiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
            baiduMap.setBuildingsEnabled(false);
            if (points == null || points.size() == 0) {
                return markers;
            }
            String   pathIndex = (index + 1) + "";
            View     startView = View.inflate(mapView.getContext(), R.layout.baidu_start_end, null);
            View     iv_start  = startView.findViewById(R.id.baidu_start_end_iv);
            TextView tv_start  = (TextView) startView.findViewById(R.id.baidu_start_end_tv);
            iv_start.setBackgroundResource(R.drawable.icon_start_walk);
            tv_start.setText(pathIndex);
            BitmapDescriptor startBitmap = BitmapDescriptorFactory.fromView(startView);
            View             endView     = View.inflate(mapView.getContext(), R.layout.baidu_start_end, null);
            View             iv_end      = endView.findViewById(R.id.baidu_start_end_iv);
            TextView         tv_end      = (TextView) endView.findViewById(R.id.baidu_start_end_tv);
            iv_end.setBackgroundResource(R.drawable.icon_arrive_walk);
            if (TextUtils.isEmpty(endText)) {
                tv_end.setText(pathIndex);
            } else {
                tv_end.setText(endText);
            }
            BitmapDescriptor endBitmap  = BitmapDescriptorFactory.fromView(endView);
            String           startTitle = "起点-" + pathIndex;
            String           endTitle   = "终点-" + pathIndex;
            if (points.size() == 1) {
                OverlayOptions startOptions = new MarkerOptions().position(points.get(0)).icon(startBitmap)
                        .zIndex(9).draggable(true).title(startTitle);
                Overlay overlay = baiduMap.addOverlay(startOptions);
                markers[0] = (Marker) overlay;
                updateMapStatus(baiduMap, points.get(0), ZOOM_BAIDU_MAP);
                return markers;
            }
            LatLng startPoint;
            LatLng endPoint;
            if (ascend) {
                startPoint = points.get(0);
                endPoint = points.get(points.size() - 1);
            } else {
                startPoint = points.get(points.size() - 1);
                endPoint = points.get(0);
            }

            // 添加起点图标
            OverlayOptions startOptions = new MarkerOptions()
                    .position(startPoint).icon(startBitmap)
                    .zIndex(9).draggable(true).title(startTitle);
            // 添加终点图标
            OverlayOptions endOptions = new MarkerOptions().position(endPoint)
                    .icon(endBitmap).zIndex(9).draggable(true).title(endTitle);

            // 添加路线（轨迹）
            OverlayOptions polylineOptions = new PolylineOptions().width(10)
                    .color(color).points(points);

            Marker startMarker = (Marker) baiduMap.addOverlay(startOptions);
            Marker endMarker   = (Marker) baiduMap.addOverlay(endOptions);
            markers[0] = startMarker;
            markers[1] = endMarker;
            //路线覆盖物
            Overlay polylineOverlay = baiduMap.addOverlay(polylineOptions);
            return markers;
        }
    }

    private static void updateMapStatus(BaiduMap baiduMap, LatLng location, float zoom) {
        MapStatus.Builder builder = new MapStatus.Builder();
        MapStatusUpdate   update  = MapStatusUpdateFactory.newMapStatus(builder.target(location).zoom(zoom).build());
        if (baiduMap.getProjection() != null) {
            baiduMap.animateMapStatus(update);
        } else if (baiduMap.getMapStatus() == null) {
            baiduMap.setMapStatus(update);
        }
        baiduMap.setMapStatus(update);
    }

    public void refreshMap(BaiduMap baiduMap) {
        LatLng location = baiduMap.getMapStatus().target;
        float  zoom     = baiduMap.getMapStatus().zoom - 1.0f;
        updateMapStatus(baiduMap, location, zoom);
    }
}
