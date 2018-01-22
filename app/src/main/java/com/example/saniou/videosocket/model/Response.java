package com.example.saniou.videosocket.model;

public class Response {

    private String status;
    private int type;
    private String devid;
    private String devip;
    private int channelid;
    private int streamtype;
    private int cameraid;
    private int taskid;
    private StreamInfo streamInfo;

    public int getCameraid() {
        return cameraid;
    }

    public void setCameraid(int cameraid) {
        this.cameraid = cameraid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getDevid() {
        return devid;
    }

    public void setDevid(String devid) {
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

    public StreamInfo getStreamInfo() {
        return streamInfo;
    }

    public void setStreamInfo(StreamInfo streamInfo) {
        this.streamInfo = streamInfo;
    }

    public int getTaskid() {
        return taskid;
    }

    public void setTaskid(int taskid) {
        this.taskid = taskid;
    }

    @Override
    public String toString() {
        return "Response{" +
                "status='" + status + '\'' +
                ", type=" + type +
                ", devid='" + devid + '\'' +
                ", devip='" + devip + '\'' +
                ", channelid=" + channelid +
                ", streamtype=" + streamtype +
                ", cameraid=" + cameraid +
                ", taskid=" + taskid +
                ", streamInfo=" + streamInfo +
                '}';
    }
}
