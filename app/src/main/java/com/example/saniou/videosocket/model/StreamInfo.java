package com.example.saniou.videosocket.model;

public class StreamInfo {
    private int videowidth;
    private int videoheight;
    private int framerate;
    private int coderate;
    private int audiochannelcount;
    private int audiobitPersample;
    private int audiosamplepersec;
    private int audiobitspersec;

    public int getVideowidth() {
        return videowidth;
    }

    public void setVideowidth(int videowidth) {
        this.videowidth = videowidth;
    }

    public int getFramerate() {
        return framerate;
    }

    public void setFramerate(int framerate) {
        this.framerate = framerate;
    }

    public int getCoderate() {
        return coderate;
    }

    public void setCoderate(int coderate) {
        this.coderate = coderate;
    }

    public int getAudiochannelcount() {
        return audiochannelcount;
    }

    public void setAudiochannelcount(int audiochannelcount) {
        this.audiochannelcount = audiochannelcount;
    }

    public int getAudiosamplepersec() {
        return audiosamplepersec;
    }

    public void setAudiosamplepersec(int audiosamplepersec) {
        this.audiosamplepersec = audiosamplepersec;
    }

    public int getAudiobitspersec() {
        return audiobitspersec;
    }

    public void setAudiobitspersec(int audiobitspersec) {
        this.audiobitspersec = audiobitspersec;
    }

    public int getVideoheight() {
        return videoheight;
    }

    public void setVideoheight(int videoheight) {
        this.videoheight = videoheight;
    }

    public int getAudiobitPersample() {
        return audiobitPersample;
    }

    public void setAudiobitPersample(int audiobitPersample) {
        this.audiobitPersample = audiobitPersample;
    }
}
