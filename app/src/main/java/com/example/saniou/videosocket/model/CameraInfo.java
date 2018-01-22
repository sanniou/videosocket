package com.example.saniou.videosocket.model;

/**
 * Created by songgx on 2016/8/3.
 * 相机的详细信息
 */
public class CameraInfo {

    private String deviceuserip;//服务器ip
    private int channelno;//摄像头通道号
    private String forwarderserverip;//转发服务器ip
    private int deviceid;//第三方设备id
    private int forwarderserverdataport;//转发服务器数据端口号
    private int cameraid;//摄像头id
    private int forwarderservercmdport;//转发服务器命令端口号
    private String gatewaycode;//网关编码
    private String name;

    public String getDeviceuserip() {
        return deviceuserip;
    }

    public void setDeviceuserip(String deviceuserip) {
        this.deviceuserip = deviceuserip;
    }

    public int getChannelno() {
        return channelno;
    }

    public void setChannelno(int channelno) {
        this.channelno = channelno;
    }

    public String getForwarderserverip() {
        return forwarderserverip;
    }

    public void setForwarderserverip(String forwarderserverip) {
        this.forwarderserverip = forwarderserverip;
    }

    public int getDeviceid() {
        return deviceid;
    }

    public void setDeviceid(int deviceid) {
        this.deviceid = deviceid;
    }

    public int getForwarderserverdataport() {
        return forwarderserverdataport;
    }

    public void setForwarderserverdataport(int forwarderserverdataport) {
        this.forwarderserverdataport = forwarderserverdataport;
    }

    public int getCameraid() {
        return cameraid;
    }

    public void setCameraid(int cameraid) {
        this.cameraid = cameraid;
    }

    public int getForwarderservercmdport() {
        return forwarderservercmdport;
    }

    public void setForwarderservercmdport(int forwarderservercmdport) {
        this.forwarderservercmdport = forwarderservercmdport;
    }

    public String getGatewaycode() {
        return gatewaycode;
    }

    public void setGatewaycode(String gatewaycode) {
        this.gatewaycode = gatewaycode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "CameraInfo{" +
                "deviceuserip='" + deviceuserip + '\'' +
                ", channelno=" + channelno +
                ", forwarderserverip='" + forwarderserverip + '\'' +
                ", deviceid=" + deviceid +
                ", forwarderserverdataport=" + forwarderserverdataport +
                ", cameraid=" + cameraid +
                ", forwarderservercmdport=" + forwarderservercmdport +
                ", gatewaycode='" + gatewaycode + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}

