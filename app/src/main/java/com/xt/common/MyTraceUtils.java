package com.xt.common;

import androidx.annotation.NonNull;

import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.CoordinateConverter;
import com.baidu.trace.LBSTraceClient;
import com.baidu.trace.Trace;
import com.baidu.trace.api.analysis.DrivingBehaviorRequest;
import com.baidu.trace.api.analysis.OnAnalysisListener;
import com.baidu.trace.api.analysis.StayPointRequest;
import com.baidu.trace.api.entity.EntityListRequest;
import com.baidu.trace.api.entity.FilterCondition;
import com.baidu.trace.api.entity.OnEntityListener;
import com.baidu.trace.api.track.HistoryTrackRequest;
import com.baidu.trace.api.track.LatestPointRequest;
import com.baidu.trace.api.track.OnTrackListener;
import com.baidu.trace.api.track.SupplementMode;
import com.baidu.trace.api.track.TrackPoint;
import com.baidu.trace.model.CoordType;
import com.baidu.trace.model.OnTraceListener;
import com.baidu.trace.model.ProcessOption;
import com.baidu.trace.model.PushMessage;
import com.baidu.trace.model.TraceLocation;
import com.baidu.trace.model.TransportMode;
import com.xt.samplebaidumap.MyApp;

import java.util.ArrayList;
import java.util.List;

import static com.xt.common.MyTraceUtils.TraceServiceManager.getServiceId;
import static com.xt.common.MyTraceUtils.TraceServiceManager.getTraceClient;

/**
 * @author xt on 2020/7/15 11:07
 * 使用百度鹰眼SDK的工具类
 */
public class MyTraceUtils {

    public static class TraceServiceManager {
        private static LBSTraceClient  sTraceClient;
        private static OnTraceListener sOnTraceListener;
        private static OnTraceListener sOnTraceListener1;
        private static Trace           sTrace;

        /**
         * 轨迹服务ID
         *
         * @return
         */
        static long getServiceId() {
            return 0;
        }

        public static void startTrace() {
            // 轨迹服务ID
            // 设备标识
            String entityName = "myTrace";
            // 是否需要对象存储服务，默认为：false，关闭对象存储服务。注：鹰眼 Android SDK v3.0以上版本支持随轨迹上传图像等对象数据，若需使用此功能，该参数需设为 true，且需导入bos-android-sdk-1.0.2.jar。
            boolean isNeedObjectStorage = false;
            // 初始化轨迹服务
            sTrace = new Trace(getServiceId(), entityName, isNeedObjectStorage);
            // 初始化轨迹服务客户端
            sTraceClient = new LBSTraceClient(MyApp.sApp);
            sOnTraceListener = new OnTraceListener() {
                @Override
                public void onBindServiceCallback(int i, String s) {

                }

                @Override
                public void onStartTraceCallback(int i, String s) {

                }

                @Override
                public void onStopTraceCallback(int i, String s) {

                }

                @Override
                public void onStartGatherCallback(int i, String s) {

                }

                @Override
                public void onStopGatherCallback(int i, String s) {

                }

                @Override
                public void onPushCallback(byte b, PushMessage pushMessage) {

                }

                @Override
                public void onInitBOSCallback(int i, String s) {

                }
            };
            sOnTraceListener1 = new OnTraceListener() {
                @Override
                public void onBindServiceCallback(int i, String s) {

                }

                @Override
                public void onStartTraceCallback(int i, String s) {

                }

                @Override
                public void onStopTraceCallback(int i, String s) {

                }

                @Override
                public void onStartGatherCallback(int i, String s) {

                }

                @Override
                public void onStopGatherCallback(int i, String s) {

                }

                @Override
                public void onPushCallback(byte b, PushMessage pushMessage) {

                }

                @Override
                public void onInitBOSCallback(int i, String s) {

                }
            };
            sTraceClient.startTrace(sTrace, sOnTraceListener);
            sTraceClient.startGather(sOnTraceListener1);
        }

        public static void stopTrace() {
            if (sTraceClient != null) {
                sTraceClient.stopTrace(sTrace, sOnTraceListener);
                sTraceClient.stopGather(sOnTraceListener);
            }
        }

        static LBSTraceClient getTraceClient() {
            return sTraceClient;
        }
    }

    /**
     * 将轨迹实时定位点转换为地图坐标
     *
     * @param location
     * @return
     */
    public static LatLng convertTraceLocation2Map(TraceLocation location) {
        if (null == location) {
            return null;
        }
        double latitude  = location.getLatitude();
        double longitude = location.getLongitude();
        if (Math.abs(latitude - 0.0) < 0.000001 && Math.abs(longitude - 0.0) < 0.000001) {
            return null;
        }
        LatLng currentLatLng = new LatLng(latitude, longitude);
        if (CoordType.wgs84 == location.getCoordType()) {
            LatLng              sourceLatLng = currentLatLng;
            CoordinateConverter converter    = new CoordinateConverter();
            converter.from(CoordinateConverter.CoordType.GPS);
            converter.coord(sourceLatLng);
            currentLatLng = converter.convert();
        }
        return currentLatLng;
    }

    /**
     * 将地图坐标转换轨迹坐标
     *
     * @param latLng
     * @return
     */
    public static com.baidu.trace.model.LatLng convertMap2Trace(LatLng latLng) {
        return new com.baidu.trace.model.LatLng(latLng.latitude, latLng.longitude);
    }

    /**
     * 将轨迹坐标对象转换为地图坐标对象
     *
     * @param traceLatLng
     * @return
     */
    public static LatLng convertTrace2Map(com.baidu.trace.model.LatLng traceLatLng) {
        return new LatLng(traceLatLng.latitude, traceLatLng.longitude);
    }

    @NonNull
    public static List<TrackPoint> getTrackPoints(List<TrackPoint> trackPoints) {
        /**
         * 轨迹点集合
         */
        List<TrackPoint> trackPointList = new ArrayList<>();
        if (null != trackPoints) {
            for (TrackPoint trackPoint : trackPoints) {
                if (!MyBaiduMapUtils.isZeroPoint(trackPoint.getLocation().getLatitude(), trackPoint.getLocation().getLongitude())) {
                    trackPointList.add(trackPoint);
                }
            }
        }
        return trackPointList;
    }

    public static List<LatLng> getLatLngList(List<TrackPoint> trackPoints) {
        List<LatLng> latLngs = new ArrayList<>();
        if (trackPoints != null) {
            for (TrackPoint item : trackPoints) {
                latLngs.add(convertTrace2Map(item.getLocation()));
            }
        }
        return latLngs;
    }

    public static List<List<TrackPoint>> splitTrackPoints(List<TrackPoint> trackPoints) {
        if (trackPoints != null) {
            int trackPointSize = trackPoints.size();
            if (trackPointSize > 0) {
                List<List<TrackPoint>> paths           = new ArrayList<>();
                List<TrackPoint>       tempTrackPoints = new ArrayList<>();
                for (int i = 0; i <= trackPointSize - 1; i++) {
                    TrackPoint currentTrackPoint = trackPoints.get(i);
                    tempTrackPoints.add(currentTrackPoint);
                    if (i == trackPointSize - 1) {
                        paths.add(new ArrayList<>(tempTrackPoints));
                    } else {
                        long       currentLocTime = currentTrackPoint.getLocTime();
                        TrackPoint nextTrackPoint = trackPoints.get(i + 1);
                        long       nextLocTime    = nextTrackPoint.getLocTime();
                        if ((nextLocTime * 1000 - currentLocTime * 1000) > 5 * 60 * 1000) {
                            if (tempTrackPoints.size() > 0) {
                                paths.add(new ArrayList<>(tempTrackPoints));
                            }
                            tempTrackPoints.clear();
                        }
                    }
                }
                return paths;
            }
        }
        return new ArrayList<>();
    }

    public static List<List<LatLng>> getLatLngListList(List<List<TrackPoint>> trackPoints) {
        List<List<LatLng>> latLngListList = new ArrayList<>();
        if (trackPoints != null) {
            for (List<TrackPoint> item : trackPoints) {
                latLngListList.add(getLatLngList(item));
            }
        }
        return latLngListList;
    }

    public static class TrackQueryUtils {
        private ProcessOption getProcessOption(boolean isNeedDenoise, boolean isNeedVacuate, boolean isNeedMapMatch, int radiusThreshold, TransportMode transportMode) {
            // 创建纠偏选项实例
            ProcessOption processOption = new ProcessOption();
            // 设置需要去噪
            processOption.setNeedDenoise(isNeedDenoise);
            // 设置需要抽稀
            processOption.setNeedVacuate(isNeedVacuate);
            // 设置需要绑路
            processOption.setNeedMapMatch(isNeedMapMatch);
            // 设置精度过滤值(定位精度大于100米的过滤掉)
            processOption.setRadiusThreshold(radiusThreshold);
            // 设置交通方式为驾车
            processOption.setTransportMode(transportMode);
            return processOption;
        }

        /**
         * @param entityName      设备标识
         * @param startTime
         * @param endTime
         * @param onTrackListener
         */
        public void queryHistoryTrack(String entityName, long startTime, long endTime, OnTrackListener onTrackListener) {
            //轨迹上传的同时， 可通过轨迹查询接口获取实时轨迹信息：
            // 请求标识
            int tag = 1;
            // 轨迹服务ID
            long serviceId = getServiceId();
            // 创建历史轨迹请求实例
            HistoryTrackRequest historyTrackRequest = new HistoryTrackRequest(tag, serviceId, entityName);

            //设置轨迹查询起止时间
            // 开始时间(单位：秒)
//        long startTime = System.currentTimeMillis() / 1000 - 12 * 60 * 60;
            // 结束时间(单位：秒)
//        long endTime = System.currentTimeMillis() / 1000;
            // 设置开始时间
            historyTrackRequest.setStartTime(startTime);
            historyTrackRequest.setPageSize(5000);
            // 设置结束时间
            historyTrackRequest.setEndTime(endTime);
            //是否返回纠偏后的轨迹
            historyTrackRequest.setProcessed(true);
            // 设置纠偏选项
            ProcessOption processOption = getProcessOption(true, true, true, 20, TransportMode.driving);
//        ProcessOption processOption = getProcessOption(true, true, false, 20);
            historyTrackRequest.setProcessOption(processOption);

            // 设置里程填充方式为驾车
            historyTrackRequest.setSupplementMode(SupplementMode.driving);
            // 查询历史轨迹
            getTraceClient().queryHistoryTrack(historyTrackRequest, onTrackListener);
        }

        /**
         * @param entityName      设备标识
         * @param onTrackListener
         */
        public void queryLatestPoint(String entityName, OnTrackListener onTrackListener) {
            //轨迹上传的同时， 可通过轨迹查询接口获取实时轨迹信息：
            // 请求标识
            int tag = 2;
            // 轨迹服务ID
            long serviceId = getServiceId();
            // 创建历史轨迹请求实例
            LatestPointRequest latestPointRequest = new LatestPointRequest(tag, serviceId, entityName);
            latestPointRequest.setCoordTypeOutput(CoordType.bd09ll);
            //设置轨迹查询起止时间
            // 开始时间(单位：秒)
//        long startTime = System.currentTimeMillis() / 1000 - 12 * 60 * 60;
            // 结束时间(单位：秒)
//        long endTime = System.currentTimeMillis() / 1000;
            // 设置纠偏选项
            ProcessOption processOption = new ProcessOption();
            processOption.setTransportMode(TransportMode.driving);
            latestPointRequest.setProcessOption(processOption);
            getTraceClient().queryLatestPoint(latestPointRequest, onTrackListener);
        }

        /**
         * @param entityNames    设备标识 entity标识列表（多个entityName，以英文逗号"," 分割）
         * @param entityListener
         */
        public void queryEntityList(List<String> entityNames, OnEntityListener entityListener) {
            //轨迹上传的同时， 可通过轨迹查询接口获取实时轨迹信息：
            // 请求标识
            int tag = 3;
            // 轨迹服务ID
            long serviceId = getServiceId();
            //检索条件（格式为 : "key1=value1,key2=value2,....."）
//        Map<String,String> columns = new HashMap<>();
            //返回结果的类型（0 : 返回全部结果，1 : 只返回entityName的列表）
            int returnType = 0;
            //活跃时间，UNIX时间戳（指定该字段时，返回从该时间点之后仍有位置变动的entity的实时点集合）
//        int activeTime = (int) (System.currentTimeMillis() / 1000 - 12 * 60 * 60);
//        int activeTime = (int) (System.currentTimeMillis() / 1000);
            //分页大小
            int pageSize = 100;
            //分页索引
            int               pageIndex         = 0;
            EntityListRequest entityListRequest = new EntityListRequest(tag, serviceId);
            FilterCondition   filterCondition   = new FilterCondition();
            filterCondition.setEntityNames(entityNames);
//        filterCondition.setColumns(columns);
//        filterCondition.setActiveTime(activeTime);
            entityListRequest.setFilterCondition(filterCondition);
            entityListRequest.setPageSize(pageSize);
            entityListRequest.setPageIndex(pageIndex);
            getTraceClient().queryEntityList(entityListRequest, entityListener);
        }

        /**
         * @param entityName         设备标识
         * @param startTime
         * @param endTime
         * @param onAnalysisListener
         */
        public void queryDrivingBehavior(String entityName, long startTime, long endTime, OnAnalysisListener onAnalysisListener) {
            // 请求标识
            int tag = 4;
            // 轨迹服务ID
            long                   serviceId              = getServiceId();
            DrivingBehaviorRequest drivingBehaviorRequest = new DrivingBehaviorRequest(tag, serviceId, entityName);
            drivingBehaviorRequest.setStartTime(startTime / 1000);
            drivingBehaviorRequest.setEndTime(endTime / 1000);
            drivingBehaviorRequest.setCoordTypeOutput(CoordType.bd09ll);
//        ThresholdOption thresholdOption = new ThresholdOption();
//        drivingBehaviorRequest.setThresholdOption(thresholdOption);
            drivingBehaviorRequest.setProcessOption(
                    getProcessOption(
                            true,
                            true,
                            true,
                            20,
                            TransportMode.driving));
            getTraceClient().queryDrivingBehavior(drivingBehaviorRequest, onAnalysisListener);
        }

        /**
         * @param entityName         设备标识
         * @param startTime
         * @param endTime
         * @param onAnalysisListener
         */
        public void queryStayPoint(String entityName, long startTime, long endTime, OnAnalysisListener onAnalysisListener) {
            // 请求标识
            int tag = 5;
            // 轨迹服务ID
            long             serviceId        = getServiceId();
            StayPointRequest stayPointRequest = new StayPointRequest(tag, serviceId, entityName);
            stayPointRequest.setStartTime(startTime);
            stayPointRequest.setEndTime(endTime);
            stayPointRequest.setCoordTypeOutput(CoordType.bd09ll);
            stayPointRequest.setStayRadius(20);
            stayPointRequest.setStayTime(100);
            // 设置纠偏选项
            ProcessOption processOption = getProcessOption(true, true, true, 20, TransportMode.driving);
//        ProcessOption processOption = getProcessOption(true, true, false, 20);
            stayPointRequest.setProcessOption(processOption);
            getTraceClient().queryStayPoint(stayPointRequest, onAnalysisListener);
        }
    }
}
