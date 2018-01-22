package com.example.saniou.videosocket.model;

/**
 * Created by songgx on 2016/10/24.
 * 历史视频请求实体
 */

public class HistoryRequest {

    private int type;
    private long begintime;
    private long endtime;
    private HistoryStreamSet historyStreamSet;

    public HistoryStreamSet getHistoryStreamSet() {
        return historyStreamSet;
    }

    public void setHistoryStreamSet(HistoryStreamSet historyStreamSet) {
        this.historyStreamSet = historyStreamSet;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getBegintime() {
        return begintime;
    }

    public void setBegintime(long begintime) {
        this.begintime = begintime;
    }

    public long getEndtime() {
        return endtime;
    }

    public void setEndtime(long endtime) {
        this.endtime = endtime;
    }
}
