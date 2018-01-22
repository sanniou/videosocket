package com.example.saniou.videosocket.model;

/**
 * Created by songgx on 2016/10/24.
 * 视频stream实体
 */

public class stream {

    private int devid;
    private String devip;
    private int channelid;
    private int streamtype;
    private int mediatype;
    private int cameraid;
    private int playindex;
    private StreamInfo streaminfo;

    public int getCameraid() {
        return cameraid;
    }

    public void setCameraid(int cameraid) {
        this.cameraid = cameraid;
    }

    public int getMediatype() {
        return mediatype;
    }

    public void setMediatype(int mediatype) {
        this.mediatype = mediatype;
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

    public StreamInfo getStreaminfo() {
        return streaminfo;
    }

    public void setStreaminfo(StreamInfo streaminfo) {
        this.streaminfo = streaminfo;
    }

    public int getPlayindex() {
        return playindex;
    }

    public void setPlayindex(int playindex) {
        this.playindex = playindex;
    }
}
