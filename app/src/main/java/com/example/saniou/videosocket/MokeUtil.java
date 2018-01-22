package com.example.saniou.videosocket;

import com.example.saniou.videosocket.model.CameraInfo;

public class MokeUtil {

    private static int[] cameraIds = new int[]{1, 2, 4, 5, 6, 8};
    private static int[] channels = new int[]{0, 0, 1, 2, 3, 0};
    private static int[] deviceIds = new int[]{1, 2, 3, 3, 3, 3};
    private static String[] names = new String[]{"hk_141", "dh_202", "002", "003", "004", "005"};

    public static CameraInfo createCamera(int index) {
        CameraInfo cameraInfo = new CameraInfo();
        cameraInfo.setName(names[index]);
        cameraInfo.setCameraid(cameraIds[index]);
        cameraInfo.setChannelno(channels[index]);
        cameraInfo.setDeviceid(deviceIds[index]);
        cameraInfo.setDeviceuserip("192.168.3.202");
        cameraInfo.setForwarderservercmdport(10101);
        cameraInfo.setForwarderserverdataport(10102);
        cameraInfo.setForwarderserverip("192.168.3.150");
        cameraInfo.setGatewaycode("zrhx_test");
        return cameraInfo;
    }

    public static CameraInfo createCamera(String name) {
        for (int i = 0; i < names.length; i++) {
            if (names[i].equals(name)) {
                return createCamera(i);
            }
        }
        return null;
    }
}
