package com.example.saniou.videosocket.model;

/**
 * Created by songgx on 2016/10/24.
 * 历史视频的响应实体
 */

public class HistoryResponse {

    private int type;
    private String status;
    private String describe;
    private int playtaskid;
    private int playindex;
    private stream stream;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescribe() {
        return describe;
    }

    public void setDescribe(String describe) {
        this.describe = describe;
    }

    public int getPlaytaskid() {
        return playtaskid;
    }

    public void setPlaytaskid(int playtaskid) {
        this.playtaskid = playtaskid;
    }

    public stream getStream() {
        return stream;
    }

    public void setStream(stream stream) {
        this.stream = stream;
    }

    public int getPlayindex() {
        return playindex;
    }

    public void setPlayindex(int playindex) {
        this.playindex = playindex;
    }
}
