package com.example.saniou.videosocket.model;

/*
    message head
 **/
public class MsgHead {

    /**视频流XML协议包体*/
    private byte[] tag=new byte[4];//命令开头的标识
    private int cmdType ;//四个字节的命令类型
    private int length ;//四个字节的长度
    private String xmlString ;//
    public MsgHead(int cmdType , int length, String xmlString)
    {
        tag[0]='z';
        tag[1]='r';
        tag[2]='h';
        tag[3]='x';
        this.cmdType = cmdType ;
        this.length = length ;
        this.xmlString = xmlString ;
    }

    public byte[] getTag() {
        return tag;
    }

    public void setTag(byte[] tag) {
        this.tag = tag;
    }

    public int getCmdType() {
        return cmdType;
    }

    public void setCmdType(int cmdType) {
        this.cmdType = cmdType;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String getXmlString() {
        return xmlString;
    }

    public void setXmlString(String xmlString) {
        this.xmlString = xmlString;
    }
}
