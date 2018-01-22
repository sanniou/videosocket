package com.example.saniou.videosocket;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.SparseArray;
import android.util.Xml;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.Surface;
import com.zrhx.base.utils.LogUtils;
import com.example.saniou.videosocket.model.CameraInfo;
import com.example.saniou.videosocket.model.HistoryRequest;
import com.example.saniou.videosocket.model.HistoryResponse;
import com.example.saniou.videosocket.model.HistoryStreamSet;
import com.example.saniou.videosocket.model.MsgHead;
import com.example.saniou.videosocket.model.Request;
import com.example.saniou.videosocket.model.Response;
import com.example.saniou.videosocket.model.StreamInfo;
import com.example.saniou.videosocket.model.stream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class DataProcess {

    private static final int MAX_LENGTH = 1024 * 4;
    private static Integer playtaskidMap;
    private static Socket dataSocket;
    public static SparseArray<Surface> sSurfaceMap = new SparseArray<>();
    private static CameraInfo sCameraInfo;
    private static DataProcess3.DataProcessCallBack mCallBack;

    public static void setCallBack(DataProcess3.DataProcessCallBack callBack) {
        mCallBack = callBack;
    }

    public static void destroyAll() {
        playtaskidMap = null;
        sSurfaceMap.clear();
        try {
            Socket socket = dataSocket;
            if (socket != null && socket.isConnected()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        destroy();
    }

    /**
     * 第一步，连接命令端口，发送数据
     * 实时视频初始化播放
     */
    public static void initRealTimePlayVideo(@NonNull CameraInfo cameraInfo)
            throws Exception {
        Socket cmdSocket = new Socket();
        cmdSocket.setSoTimeout(5000);
        LogUtils.e(cameraInfo.toString());
        sCameraInfo = cameraInfo;
        cmdSocket.connect(new InetSocketAddress(cameraInfo.getForwarderserverip(),
                cameraInfo.getForwarderservercmdport()), 10 * 1000);
        if (!cmdSocket.isConnected()) {
            throw new IllegalStateException("VIDEO_GET_FAILED");
        }
        sendOpenMediaRealTimeRequest(cmdSocket, Command.CMD_MDU_STREAM_OPEN, cameraInfo);
        receiveRealTimeInitMsg(cmdSocket, cameraInfo);
        cmdSocket.close();
        playVideo();
    }

    /**
     * 发送打开视频命令请求
     */
    public static void sendOpenMediaRealTimeRequest(Socket socket, int cmdType,
                                                    CameraInfo cameraInfo) throws IOException {
        //封装请求实体数据
        Request cmdRequest = new Request();
        cmdRequest.setType(cmdType);
        cmdRequest.setDevid(cameraInfo.getDeviceid());
        cmdRequest.setDevip(cameraInfo.getForwarderserverip());
        cmdRequest.setChannelid(cameraInfo.getChannelno());
        cmdRequest.setStreamtype(Command.STREAM_TYPE_MAIN);
        cmdRequest.setCameraid(cameraInfo.getCameraid());
        cmdRequest.setTaskid(0);
        cmdRequest.setGatewaycode(cameraInfo.getGatewaycode());
        //组装报文
        String xmlString = setRealTimeXmlBody(cmdRequest);
        LogUtils.e("xmlString\n" + xmlString);
        MsgHead msgHead = new MsgHead(cmdType, xmlString.length(), xmlString);
        sendMessage(socket, msgHead, "real");
    }

    /**
     * 接收实时数据并进行配置
     */
    public static void receiveRealTimeInitMsg(Socket socket, CameraInfo cameraInfo)
            throws IOException, XmlPullParserException {
        Response cmdResponse = receiveRealTimeMediaCmd(socket);
        StreamInfo streaminfo = cmdResponse.getStreamInfo();
        if (streaminfo == null) {
            throw new RuntimeException("VIDEO_GET_FAILED");
        }
        int videoWidth = streaminfo.getVideowidth();
        int videoHeight = streaminfo.getVideoheight();
        mCallBack.onSizeDefine(videoWidth,videoHeight);
        init(videoWidth, videoHeight);
        LogUtils.e("开始连接数据流");
        //链接视频流socket
        Socket dataSocket = new Socket(cameraInfo.getForwarderserverip(),
                cameraInfo.getForwarderserverdataport());
        //设置socket 10秒超时
        dataSocket.setSoTimeout(10 * 1000);
        if (!dataSocket.isConnected()) {
            throw new RuntimeException("VIDEO_GET_FAILED");
        }
        DataProcess.dataSocket = dataSocket;
    }

    private static void playVideo() throws Exception {
        CameraInfo cameraInfo = sCameraInfo;
        Socket dataSocket = DataProcess.dataSocket;
        sendOpenMediaRealTimeRequest(dataSocket, Command.CMD_MDU_STREAM_DESCRIBE,
                cameraInfo);
        receiveMediaData(dataSocket, "realData");
    }

    /**
     * @param socket  创建连接的socket
     * @param msgHead 消息头
     *                发送cmd命令消息
     */
    public static void sendMessage(Socket socket, MsgHead msgHead, String flag) throws IOException {
        if (socket.isConnected()) {
            // 把发送消息放入w_buff中
            byte[] bXmlBody = msgHead.getXmlString().getBytes();
            int strLen = bXmlBody.length;
            ByteBuffer wBuff = ByteBuffer.allocate(MAX_LENGTH);
            if (strLen < 65527) {
                wBuff.clear();
                wBuff.order(ByteOrder.BIG_ENDIAN);
                int cmdType = msgHead.getCmdType();
                int length = msgHead.getLength();
                wBuff.put(msgHead.getTag());
                wBuff.putInt(cmdType);
                wBuff.putInt(length);
                wBuff.put(bXmlBody);
                wBuff.flip();
            } else {
                wBuff.clear();
                wBuff.order(ByteOrder.BIG_ENDIAN);
                int cmdType = msgHead.getCmdType();
                int length = msgHead.getLength();
                wBuff.put(msgHead.getTag());
                wBuff.putInt(cmdType);
                wBuff.putInt(length);
                wBuff.put(bXmlBody);
                wBuff.flip();
            }
            // 发送消息
            byte[] writeData = wBuff.array();
            if (!socket.isOutputShutdown()) {
                OutputStream outputStream = socket.getOutputStream();
                outputStream.write(writeData);
                wBuff.clear();
            }
        }
    }

    /**
     * @param socket 读取数据的socket
     *               读取server端发回的消息, 接收cmd命令返回的初始化参数
     */
    public static Response receiveRealTimeMediaCmd(Socket socket)
            throws IOException, XmlPullParserException {
        byte[] bytes = new byte[MAX_LENGTH];
        Response cmdResponse;
        if (socket.isInputShutdown()) {
            throw new IOException("socket is input shutdown");
        }
        socket.getInputStream().read(bytes);
        ByteBuffer rBuff = ByteBuffer.wrap(bytes);
        rBuff.order(ByteOrder.BIG_ENDIAN);
        int cmdType;
        int length;
        byte[] tag = new byte[4];
        rBuff.get(tag);
        cmdType = rBuff.getInt();
        length = rBuff.getInt();
        byte[] body = new byte[length];
        rBuff.get(body);
        String xmlBody = new String(body, "utf-8");
        LogUtils.e(cmdType + " : xmlBody\n" + xmlBody);
        cmdResponse = parseRealTimeXml(xmlBody);
        return cmdResponse;
    }

    /**
     * 视频接收历史视频cmd命令
     */
    public static HistoryResponse receiveHistoryMediaCmd(Socket socket) {
        byte[] bytes = new byte[MAX_LENGTH];
        HistoryResponse cmdResponse = null;
        try {
            if (!socket.isInputShutdown()) {
                socket.getInputStream().read(bytes);
                ByteBuffer r_buff = ByteBuffer.wrap(bytes);
                r_buff.order(ByteOrder.BIG_ENDIAN);
                int cmdType = 0;
                int length = 0;
                byte[] tag = new byte[4];
                r_buff.get(tag);
                cmdType = r_buff.getInt();
                length = r_buff.getInt();
                byte[] body = new byte[length];
                r_buff.get(body);
                String xmlBody = new String(body, "utf-8");
                cmdResponse = parseHistoryXml(xmlBody);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return cmdResponse;
    }

    /**
     * @param socket 接收数据socket
     *               接收数据通道相关信息
     */
    public static void receiveMediaData(Socket socket, String flag) throws Exception {
        //关闭视频，切换视频的时候退出while循环
        while (true) {
            byte[] headByte = new byte[28];
            int headByteLen = 28;
            int headPosition = 0;
            long headReceiveLen = 0;
            //接收包体头信息
            while (headByteLen > 0) {
                try {
                    if (!socket.isInputShutdown()) {
                        LogUtils.e("socket开始读取字节流");
                        headReceiveLen = socket.getInputStream()
                                               .read(headByte, headPosition, headByteLen);
                        LogUtils.e(headReceiveLen + "总长度是" + headReceiveLen);
                        //网络断开,通知界面
                        if (headReceiveLen < 0) {
                            throw new Exception("socket is close");
                        }
                        //有数据还没有收到
                        if (headReceiveLen == 0) {
                            Thread.sleep(10);
                        }
                        headByteLen -= headReceiveLen;
                        headPosition += headReceiveLen;
                    } else {
                        throw new Exception("socket is close");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
            ByteBuffer headByteBuffer = ByteBuffer.wrap(headByte);
            headByteBuffer.order(ByteOrder.BIG_ENDIAN);
            int nDeviceId = headByteBuffer.getInt();
            int nChannelId = headByteBuffer.getInt();
            int nStreamId = headByteBuffer.getInt();
            int nMediaType = headByteBuffer.getInt();
            int nPacketIndex = headByteBuffer.getInt();
            int tTimeStamp = headByteBuffer.getInt();
            int bodyDataLen = headByteBuffer.getInt();
            long bodyReceiveLen;
            int bodyPosition = 0;
            if (nPacketIndex == 0xffff && bodyDataLen == 0) {
                return;
            }
            byte[] bodyByte = new byte[500 * 1024];
            while (bodyDataLen > 0) {
                if (socket.isInputShutdown()) {
                    throw new Exception("socket is close");
                }
                bodyReceiveLen = socket.getInputStream()
                                       .read(bodyByte, bodyPosition, bodyDataLen);
                //网络断开,通知界面
                if (bodyReceiveLen < 0) {
                    throw new Exception("socket is close");
                }
                //有数据还没有收到
                if (bodyReceiveLen == 0) {
                    Thread.sleep(10);
                }
                bodyDataLen -= bodyReceiveLen;
                bodyPosition += bodyReceiveLen;
            }
            notifyMediaBufferListener(bodyByte, bodyByte.length);
            LogUtils.e("socket读取字节流结束");
            LogUtils.e(headReceiveLen + "总长度是" + bodyPosition);
        }
    }

    private static void notifyMediaBufferListener(byte[] byteBuffer, int size) {
        Log.e("threadName", Thread.currentThread().getName());
        Log.e("threadCount", String.valueOf(Thread.activeCount()));
        //数据为空
        if (byteBuffer != null) {
            if ((byteBuffer[0] == 0 && byteBuffer[1] == 0 && byteBuffer[2] == 0 &&
                    byteBuffer[3] == 1 && byteBuffer[4] == 103)) {

            }
            mCallBack.onByteBuff(byteBuffer,size);
            decode(byteBuffer, size);
        }
    }

    /**
     * 发送打开视频的数据通道的命令请求
     */
    public static void sendOpenMediaHistoryRequest(Socket socket, int cmdType,
                                                   CameraInfo cameraInfo, long beginTime,
                                                   long endTime, boolean isDataRequest)
            throws IOException {
        //封装请求实体数据
        HistoryRequest request = new HistoryRequest();
        request.setBegintime(beginTime);
        request.setEndtime(endTime);
        request.setType(cmdType);
        HistoryStreamSet streamSet = new HistoryStreamSet();
        stream stream = new stream();
        stream.setChannelid(cameraInfo.getChannelno());
        stream.setDevid(cameraInfo.getDeviceid());
        stream.setDevip(cameraInfo.getDeviceuserip());
        stream.setStreamtype(Command.STREAM_TYPE_MAIN);
        stream.setMediatype(0);
        stream.setCameraid(cameraInfo.getCameraid());
        stream.setPlayindex(0);
        streamSet.setStream(stream);
        request.setHistoryStreamSet(streamSet);
        //组装报文
        String xmlString = null;
        if (!isDataRequest) {
            xmlString = setHistoryCmdXmlBody(request);
        } else {
            xmlString = setHistoryDataXmlBody(request);
        }
        MsgHead msgHead = new MsgHead(cmdType, xmlString.length(), xmlString);
        sendMessage(socket, msgHead, "history");
    }

    /**
     * @param cmdRequest 请求的xml包体参数
     *                   封装xml包体
     */

    public static String setRealTimeXmlBody(Request cmdRequest) {
        StringBuffer stringBuffer = new StringBuffer();
        String xmlHead = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";
        stringBuffer.append(xmlHead);
        stringBuffer.append("\n");

        stringBuffer.append("<request>");
        stringBuffer.append("\n");

        stringBuffer.append("<type>");
        stringBuffer.append(cmdRequest.getType());
        stringBuffer.append("</type>");
        stringBuffer.append("\n");

        stringBuffer.append("<devid>");
        stringBuffer.append(cmdRequest.getDevid());
        stringBuffer.append("</devid>");
        stringBuffer.append("\n");

        stringBuffer.append("<devip>");
        stringBuffer.append(cmdRequest.getDevip());
        stringBuffer.append("</devip>");
        stringBuffer.append("\n");

        stringBuffer.append("<channelid>");
        stringBuffer.append(cmdRequest.getChannelid());
        stringBuffer.append("</channelid>");
        stringBuffer.append("\n");

        stringBuffer.append("<streamtype>");
        stringBuffer.append(cmdRequest.getStreamtype());
        stringBuffer.append("</streamtype>");
        stringBuffer.append("\n");

        stringBuffer.append("<cameraid>");
        stringBuffer.append(cmdRequest.getCameraid());
        stringBuffer.append("</cameraid>");
        stringBuffer.append("\n");

        stringBuffer.append("<taskid>");
        stringBuffer.append(cmdRequest.getTaskid());
        stringBuffer.append("</taskid>");
        stringBuffer.append("\n");

        stringBuffer.append("<gatewaycode>");
        stringBuffer.append(cmdRequest.getGatewaycode());
        stringBuffer.append("</gatewaycode>");
        stringBuffer.append("\n");

        stringBuffer.append("</request>");
        return stringBuffer.toString();
    }

    /**
     * @param xmlBody 解析的字符串
     *                解析xml转换为response对象
     */
    private static Response parseRealTimeXml(String xmlBody)
            throws IOException, XmlPullParserException {
        Response response = null;
        StreamInfo streaminfo;
        XmlPullParser xmlParse = Xml.newPullParser();
        xmlParse.setInput(new StringReader(xmlBody));
        int evnType = xmlParse.getEventType();
        while (evnType != XmlPullParser.END_DOCUMENT) {
            switch (evnType) {
                case XmlPullParser.START_TAG:
                    String tag = xmlParse.getName();
                    if ("response".equalsIgnoreCase(tag)) {
                        response = new Response();
                        response.setStatus(xmlParse.getAttributeValue("", "status"));
                    } else if (response != null) {
                        if ("status".equalsIgnoreCase(tag)) {
                            response.setStatus(xmlParse.nextText());
                        } else if ("type".equalsIgnoreCase(tag)) {
                            response.setType(Integer.parseInt(xmlParse.nextText()));
                        } else if ("devid".equalsIgnoreCase(tag)) {
                            response.setDevid(xmlParse.nextText());

                        } else if ("devip".equalsIgnoreCase(tag)) {
                            response.setDevip(xmlParse.nextText());

                        } else if ("channelid".equalsIgnoreCase(tag)) {
                            response.setChannelid(Integer.parseInt(xmlParse.nextText()));

                        } else if ("streamtype".equalsIgnoreCase(tag)) {
                            response.setStreamtype(Integer.parseInt(xmlParse.nextText()));

                        } else if ("cameraid".equalsIgnoreCase(tag)) {
                            response.setCameraid(Integer.parseInt(xmlParse.nextText()));
                        } else if ("taskid".equalsIgnoreCase(tag)) {
                            response.setTaskid(Integer.parseInt(xmlParse.nextText()));
                        } else if ("StreamInfo".equalsIgnoreCase(tag)) {
                            streaminfo = new StreamInfo();
                            streaminfo.setVideowidth(Integer.parseInt(
                                    xmlParse.getAttributeValue("", "videowidth")));
                            streaminfo.setFramerate(Integer.parseInt(
                                    xmlParse.getAttributeValue("", "framerate")));
                            streaminfo.setCoderate(Integer.parseInt(
                                    xmlParse.getAttributeValue("", "coderate")));
                            streaminfo.setAudiochannelcount(Integer.parseInt(
                                    xmlParse.getAttributeValue("", "audiochannelcount")));
                            streaminfo.setAudiosamplepersec(Integer.parseInt(
                                    xmlParse.getAttributeValue("", "audiosamplepersec")));
                            streaminfo.setAudiobitspersec(Integer.parseInt(
                                    xmlParse.getAttributeValue("", "audiobitspersec")));
                            streaminfo.setVideoheight(Integer.parseInt(
                                    xmlParse.getAttributeValue("", "videoheight")));
                            streaminfo.setAudiobitPersample(Integer.parseInt(
                                    xmlParse.getAttributeValue("", "audiobitPersample")));
                            response.setStreamInfo(streaminfo);
                        }
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if ("response".equalsIgnoreCase(xmlParse.getName()) && response != null) {
                        LogUtils.e(response.toString());
                    }
                    break;
                default:
            }
            evnType = xmlParse.next();
        }
        return response;
    }

    /**
     * @param request 请求实体
     *                历史视频命令请求方法
     */
    public static String setHistoryCmdXmlBody(HistoryRequest request) {
        StringBuffer stringBuffer = new StringBuffer();
        String xmlHead = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";
        stringBuffer.append(xmlHead);
        stringBuffer.append("\n");

        stringBuffer.append("<request>");
        stringBuffer.append("\n");

        stringBuffer.append("<type>");
        stringBuffer.append(request.getType());
        stringBuffer.append("</type>");
        stringBuffer.append("\n");

        stringBuffer.append("<begintime>");
        stringBuffer.append(request.getBegintime());
        stringBuffer.append("</begintime>");
        stringBuffer.append("\n");

        stringBuffer.append("<endtime>");
        stringBuffer.append(request.getEndtime());
        stringBuffer.append("</endtime>");
        stringBuffer.append("\n");

        HistoryStreamSet streamSet = request.getHistoryStreamSet();
        if (streamSet != null) {
            stringBuffer.append("<streamset>");
            stringBuffer.append("\n");
            stream stream = streamSet.getStream();
            if (stream != null) {
                stringBuffer.append("<stream>");
                stringBuffer.append("\n");

                stringBuffer.append("<devid>");
                stringBuffer.append(stream.getDevid());
                stringBuffer.append("</devid>");
                stringBuffer.append("\n");

                stringBuffer.append("<devip>");
                stringBuffer.append(stream.getDevip());
                stringBuffer.append("</devip>");
                stringBuffer.append("\n");

                stringBuffer.append("<channelid>");
                stringBuffer.append(stream.getChannelid());
                stringBuffer.append("</channelid>");
                stringBuffer.append("\n");

                stringBuffer.append("<mediatype>");
                stringBuffer.append(stream.getMediatype());
                stringBuffer.append("</mediatype>");
                stringBuffer.append("\n");

                stringBuffer.append("<streamtype>");
                stringBuffer.append(stream.getStreamtype());
                stringBuffer.append("</streamtype>");
                stringBuffer.append("\n");

                stringBuffer.append("<cameraid>");
                stringBuffer.append(stream.getCameraid());
                stringBuffer.append("</cameraid>");
                stringBuffer.append("\n");

                stringBuffer.append("<playindex>");
                stringBuffer.append(stream.getPlayindex());
                stringBuffer.append("</playindex>");
                stringBuffer.append("\n");

                stringBuffer.append("</stream>");
                stringBuffer.append("\n");

                stringBuffer.append("</streamset>");
                stringBuffer.append("\n");
            }
        }
        stringBuffer.append("</request>");
        return stringBuffer.toString();
    }

    public static String setHistoryDataXmlBody(HistoryRequest request) {
        StringBuffer stringBuffer = new StringBuffer();
        String xmlHead = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";
        stringBuffer.append(xmlHead);
        stringBuffer.append("\n");

        stringBuffer.append("<request>");
        stringBuffer.append("\n");

        stringBuffer.append("<type>");
        stringBuffer.append(request.getType());
        stringBuffer.append("</type>");
        stringBuffer.append("\n");

        stringBuffer.append("<playtaskid>");
        stringBuffer.append(playtaskidMap);
        stringBuffer.append("</playtaskid>");
        stringBuffer.append("\n");

        HistoryStreamSet streamSet = request.getHistoryStreamSet();
        if (streamSet != null) {
            stream stream = streamSet.getStream();
            if (stream != null) {
                stringBuffer.append("<stream>");
                stringBuffer.append("\n");

                stringBuffer.append("<devid>");
                stringBuffer.append(stream.getDevid());
                stringBuffer.append("</devid>");
                stringBuffer.append("\n");

                stringBuffer.append("<channelid>");
                stringBuffer.append(stream.getChannelid());
                stringBuffer.append("</channelid>");
                stringBuffer.append("\n");

                stringBuffer.append("<mediatype>");
                stringBuffer.append(stream.getMediatype());
                stringBuffer.append("</mediatype>");
                stringBuffer.append("\n");

                stringBuffer.append("<streamtype>");
                stringBuffer.append(stream.getStreamtype());
                stringBuffer.append("</streamtype>");
                stringBuffer.append("\n");

                stringBuffer.append("<cameraid>");
                stringBuffer.append(stream.getCameraid());
                stringBuffer.append("</cameraid>");
                stringBuffer.append("\n");

                stringBuffer.append("<playindex>");
                stringBuffer.append(stream.getPlayindex());
                stringBuffer.append("</playindex>");
                stringBuffer.append("\n");

                stringBuffer.append("</stream>");
                stringBuffer.append("\n");
            }
        }
        stringBuffer.append("</request>");
        return stringBuffer.toString();

    }

    /**
     * @param xmlBody 解析的字符串
     *                解析xml转换为response对象
     */
    public static HistoryResponse parseHistoryXml(String xmlBody) {
        HistoryResponse response = null;
        StreamInfo streaminfo = null;
        stream stream = null;
        try {
            XmlPullParser xmlParse = Xml.newPullParser();
            xmlParse.setInput(new StringReader(xmlBody));
            int evnType = xmlParse.getEventType();
            while (evnType != XmlPullParser.END_DOCUMENT) {
                switch (evnType) {
                    case XmlPullParser.START_TAG:
                        String tag = xmlParse.getName();
                        if (tag.equalsIgnoreCase("response")) {
                            response = new HistoryResponse();
                            response.setStatus(xmlParse.getAttributeValue("", "status"));
                            response.setDescribe(xmlParse.getAttributeValue("", "describe"));
                        } else if (response != null) {
                            if (tag.equalsIgnoreCase("type")) {
                                response.setType(Integer.parseInt(xmlParse.nextText()));
                            } else if (tag.equalsIgnoreCase("playtaskid")) {
                                response.setPlaytaskid(Integer.parseInt(xmlParse.nextText()));
                                playtaskidMap = response.getPlaytaskid();
                            } else if (tag.equalsIgnoreCase("stream")) {
                                stream = new stream();
                            } else if (stream != null) {
                                if (tag.equalsIgnoreCase("devid")) {
                                    stream.setDevid(Integer.parseInt(xmlParse.nextText()));
                                } else if (tag.equalsIgnoreCase("devip")) {
                                    stream.setDevip(xmlParse.nextText());
                                } else if (tag.equalsIgnoreCase("channelid")) {
                                    stream.setChannelid(Integer.parseInt(xmlParse.nextText()));
                                } else if (tag.equalsIgnoreCase("streamtype")) {
                                    stream.setStreamtype(Integer.parseInt(xmlParse.nextText()));
                                } else if (tag.equalsIgnoreCase("cameraid")) {
                                    stream.setCameraid(Integer.parseInt(xmlParse.nextText()));
                                } else if (tag.equalsIgnoreCase("playindex")) {
                                    stream.setPlayindex(Integer.parseInt(xmlParse.nextText()));
                                } else if (tag.equalsIgnoreCase("streamInfo")) {
                                    streaminfo = new StreamInfo();
                                    streaminfo.setVideowidth(Integer.parseInt(
                                            xmlParse.getAttributeValue("", "videowidth")));
                                    streaminfo.setFramerate(Integer.parseInt(
                                            xmlParse.getAttributeValue("", "framerate")));
                                    streaminfo.setCoderate(Integer.parseInt(
                                            xmlParse.getAttributeValue("", "coderate")));
                                    streaminfo.setAudiochannelcount(Integer.parseInt(
                                            xmlParse.getAttributeValue("", "audiochannelcount")));
                                    streaminfo.setAudiosamplepersec(Integer.parseInt(
                                            xmlParse.getAttributeValue("", "audiosamplepersec")));
                                    streaminfo.setAudiobitspersec(Integer.parseInt(
                                            xmlParse.getAttributeValue("", "audiobitspersec")));
                                    streaminfo.setVideoheight(Integer.parseInt(
                                            xmlParse.getAttributeValue("", "videoheight")));
                                    streaminfo.setAudiobitPersample(Integer.parseInt(
                                            xmlParse.getAttributeValue("", "audiobitPersample")));
                                    stream.setStreaminfo(streaminfo);
                                }
                                response.setStream(stream);
                            }
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        String tag1 = xmlParse.getName();
                        if ("response".equalsIgnoreCase(tag1)) {
//                            if (Response != null) {
//                                LogUtils.e( Response.toString());
//                            }
                        }
                        break;
                    default:
                }
                evnType = xmlParse.next();
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    /**
     * 关闭视频流命令
     */
    public static void closeMediaCmdRequest(Socket socket, CameraInfo cameraInfo, int cmdType)
            throws IOException {
        /**封装请求实体数据*/
        Request cmdRequest = new Request();
        cmdRequest.setType(cmdType);
        cmdRequest.setChannelid(cameraInfo.getChannelno());
        cmdRequest.setDevid(cameraInfo.getDeviceid());
        cmdRequest.setDevip(cameraInfo.getDeviceuserip());
        cmdRequest.setStreamtype(Command.STREAM_TYPE_MAIN);
        /**组装报文*/
        String xmlString = setRealTimeXmlBody(cmdRequest);
        MsgHead msgHead = new MsgHead(cmdType, xmlString.length(), xmlString);
        sendMessage(socket, msgHead, "");
    }

    /**
     * 视频模块native方法
     */
    public static native void init(int width, int height);

    public static native void decode(byte[] byteBuffer, int size);

    public static native void destroy();
    //例子里面的方法

    public static native void nativeLowMemory();

    public static native void nativeQuit();

    public static native void nativePause();

    public static native void nativeResume();

    public static native void onNativeResize(int x, int y, int format);

    public static native int onNativePadDown(int device_id, int keycode);

    public static native int onNativePadUp(int device_id, int keycode);

    public static native void onNativeJoy(int device_id, int axis, float value);

    public static native void onNativeHat(int device_id, int hat_id, int x, int y);

    public static native void onNativeKeyDown(int keycode);

    public static native void onNativeKeyUp(int keycode);

    public static native void onNativeKeyboardFocusLost();

    public static native void onNativeTouch(int touchDevId, int pointerFingerId, int action,
                                            float x, float y, float p);

    public static native void onNativeAccel(float x, float y, float z);

    public static native void onNativeSurfaceChanged();

    public static native void onNativeSurfaceDestroyed();

    public static native void nativeFlipBuffers();

    public static native int nativeAddJoystick(int device_id, String name, int is_accelerometer,
                                               int nbuttons, int naxes, int nhats, int nballs);

    public static native int nativeRemoveJoystick(int device_id);

    public static Surface getNativeSurface() {
        return sSurfaceMap.get(0);
    }

    // Audio
    private static AudioTrack mAudioTrack;

    // Audio
    public static int audioInit(int sampleRate, boolean is16Bit, boolean isStereo,
                                int desiredFrames) {
        int channelConfig = isStereo ? AudioFormat.CHANNEL_CONFIGURATION_STEREO
                : AudioFormat.CHANNEL_CONFIGURATION_MONO;
        int audioFormat = is16Bit ? AudioFormat.ENCODING_PCM_16BIT : AudioFormat.ENCODING_PCM_8BIT;
        int frameSize = (isStereo ? 2 : 1) * (is16Bit ? 2 : 1);

        LogUtils.e("SDL audio: wanted " + (isStereo ? "stereo" : "mono") + " " +
                (is16Bit ? "16-bit" : "8-bit") + " " + (sampleRate / 1000f) + "kHz, " +
                desiredFrames + " frames buffer");

        // Let the user pick a larger buffer if they really want -- but ye
        // gods they probably shouldn't, the minimums are horrifyingly high
        // latency already
        desiredFrames = Math.max(desiredFrames, (
                AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat) + frameSize -
                        1) / frameSize);

        if (mAudioTrack == null) {
            mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelConfig,
                    audioFormat, desiredFrames * frameSize, AudioTrack.MODE_STREAM);

            // Instantiating AudioTrack can "succeed" without an exception and the track may still be invalid
            // Ref: https://android.googlesource.com/platform/frameworks/base/+/refs/heads/master/media/java/android/media/AudioTrack.java
            // Ref: http://developer.android.com/reference/android/media/AudioTrack.html#getState()

            if (mAudioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
                LogUtils.e("Failed during initialization of Audio Track");
                mAudioTrack = null;
                return -1;
            }

            mAudioTrack.play();
        }

        LogUtils.e(
                "SDL audio: got " + ((mAudioTrack.getChannelCount() >= 2) ? "stereo" : "mono") +
                        " " +
                        ((mAudioTrack.getAudioFormat() == AudioFormat.ENCODING_PCM_16BIT) ? "16-bit"
                                : "8-bit") + " " + (mAudioTrack.getSampleRate() / 1000f) + "kHz, " +
                        desiredFrames + " frames buffer");

        return 0;
    }

    public static void audioWriteShortBuffer(short[] buffer) {
        for (int i = 0; i < buffer.length; ) {
            int result = mAudioTrack.write(buffer, i, buffer.length - i);
            if (result > 0) {
                i += result;
            } else if (result == 0) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    // Nom nom
                }
            } else {
                LogUtils.e("SDL audio: error return from write(short)");
                return;
            }
        }
    }

    public static void audioWriteByteBuffer(byte[] buffer) {
        for (int i = 0; i < buffer.length; ) {
            int result = mAudioTrack.write(buffer, i, buffer.length - i);
            if (result > 0) {
                i += result;
            } else if (result == 0) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    // Nom nom
                }
            } else {
                LogUtils.e("SDL audio: error return from write(byte)");
                return;
            }
        }
    }

    public static void audioQuit() {
        if (mAudioTrack != null) {
            mAudioTrack.stop();
            mAudioTrack = null;
        }
    }

    // Input

    /**
     * @return an array which may be empty but is never null.
     */
    public static int[] inputGetInputDeviceIds(int sources) {
        int[] ids = InputDevice.getDeviceIds();
        int[] filtered = new int[ids.length];
        int used = 0;
        for (int i = 0; i < ids.length; ++i) {
            InputDevice device = InputDevice.getDevice(ids[i]);
            if ((device != null) && ((device.getSources() & sources) != 0)) {
                filtered[used++] = device.getId();
            }
        }
        return Arrays.copyOf(filtered, used);
    }

    protected static SDLJoystickHandler mJoystickHandler;

    // Joystick glue code, just a series of stubs that redirect to the SDLJoystickHandler instance
    public static boolean handleJoystickMotionEvent(MotionEvent event) {
        return mJoystickHandler.handleMotionEvent(event);
    }

    public static Thread mSDLThread;

    public static void pollInputDevices() {
        if (mSDLThread != null) {
            mJoystickHandler.pollInputDevices();
        }
    }

    public static void flipBuffers() {
        nativeFlipBuffers();
    }

    public static boolean setActivityTitle(String title) {
        LogUtils.e("titls " + title);
        // Called from SDLMain() thread and can't directly affect the view
        return true;
    }

    public static boolean sendMessage(int command, int param) {
        LogUtils.e(param + "command " + command);
        return true;
    }

    public static boolean showTextInput(int x, int y, int w, int h) {
        // Transfer the task to the main thread as a Runnable
        LogUtils.e(x + "  " + y + "  " + w + "  " + h);
        return true;
    }

    public static void handlePause() {
        nativePause();
    }

    /**
     * Called by onResume or surfaceCreated. An actual resume should be done only when the surface is ready.
     * Note: Some Android variants may send multiple surfaceChanged events, so we don't need to resume
     * every time we get one of those events, only if it comes after surfaceDestroyed
     */
    public static void handleResume() {
        nativeResume();
    }

    /* The native thread has finished */
    public static void handleNativeExit() {
        mSDLThread = null;
        LogUtils.e("handleNativeExit");
    }

    // Messages from the SDLMain thread
    static final int COMMAND_CHANGE_TITLE = 1;
    static final int COMMAND_UNUSED = 2;
    static final int COMMAND_TEXTEDIT_HIDE = 3;

    protected static final int COMMAND_USER = 0x8000;

}
