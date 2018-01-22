package com.example.saniou.videosocket.model;

/**
 * 发送media请求命令实体
 */
public class Request {

    private int type;
    private int devid;
    private String devip;
    private int channelid;
    private int streamtype;
    private int cameraid;
    private int taskid;
    private String gatewaycode;

    public String getGatewaycode() {
        return gatewaycode;
    }

    public void setGatewaycode(String gatewaycode) {
        this.gatewaycode = gatewaycode;
    }

    public int getCameraid() {
        return cameraid;
    }

    public void setCameraid(int cameraid) {
        this.cameraid = cameraid;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getDevid() {
        return devid;
    }

    public void setDevid(int devid) {
        this.devid = devid;
    }

    public String getDevip() {
        return devip;
    }

    public void setDevip(String devip) {
        this.devip = devip;
    }

    public int getChannelid() {
        return channelid;
    }

    public void setChannelid(int channelid) {
        this.channelid = channelid;
    }

    public int getStreamtype() {
        return streamtype;
    }

    public void setStreamtype(int streamtype) {
        this.streamtype = streamtype;
    }

    public int getTaskid() {
        return taskid;
    }

    public void setTaskid(int taskid) {
        this.taskid = taskid;
    }
}
