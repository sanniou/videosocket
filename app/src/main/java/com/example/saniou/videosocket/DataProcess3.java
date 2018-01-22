package com.example.saniou.videosocket;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Xml;
import android.view.Surface;
import com.example.saniou.videosocket.model.CameraInfo;
import com.example.saniou.videosocket.model.MsgHead;
import com.example.saniou.videosocket.model.Request;
import com.example.saniou.videosocket.model.Response;
import com.example.saniou.videosocket.model.StreamInfo;
import com.zrhx.base.utils.LogUtils;
import io.reactivex.schedulers.Schedulers;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class DataProcess3 {

    private static final int TIMEOUT = 4_000;
    private final int MAX_LENGTH = 1024 * 4;
    private Socket mDataSocket;
    private Surface mSurface;
    private CameraInfo mCameraInfo;
    private DataProcessCallBack mCallBack;
    private DecoderWorker mDecoderWorker;

    public void release() {
        if (mDataSocket != null && mDataSocket.isConnected()) {
            Schedulers.io().scheduleDirect(() -> {
                try {
                    closeMediaCmdRequest(mDataSocket, Command.CMD_MDU_STREAM_UNDESCRIBE,
                            mCameraInfo);
                    mDataSocket.close();
                    mDataSocket = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
        if (mDecoderWorker != null) {
            mDecoderWorker.release();
            mDecoderWorker = null;
        }
        mCameraInfo = null;
    }

    /**
     * 第一步，连接命令端口，发送数据
     * 实时视频初始化播放
     */
    public void initLiveVideo(@NonNull CameraInfo cameraInfo)
            throws IOException, XmlPullParserException {
        if (mCameraInfo != null) {
            throw new IOException("正在播放中");
        }
        LogUtils.e(cameraInfo.toString());
        mCameraInfo = cameraInfo;
        Socket cmdSocket = new Socket();
        cmdSocket.setSoTimeout(TIMEOUT);
        cmdSocket.connect(new InetSocketAddress(cameraInfo.getForwarderserverip(),
                cameraInfo.getForwarderservercmdport()), TIMEOUT);
        sendOpenMediaLiveRequest(cmdSocket, Command.CMD_MDU_STREAM_OPEN, cameraInfo);
        receiveLiveInitMsg(cmdSocket);
        closeMediaCmdRequest(cmdSocket, Command.CMD_MDU_STREAM_CLOSE, cameraInfo);
        cmdSocket.close();
        startPlay();
    }

    private void startPlay() {
        try {
            playVideo();
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof SocketTimeoutException || e instanceof SocketException) {
                // 主动断开
                if ("Socket closed".equals(e.getMessage())) {
//                    release();
                    return;
                }
                startPlay();
            }
//            release();
        }
    }

    /**
     * 发送打开视频命令请求
     */
    private void sendOpenMediaLiveRequest(Socket socket, int cmdType, CameraInfo cameraInfo)
            throws IOException {
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
        LogUtils.e("打开  " + cmdType + "\n" + xmlString);
        MsgHead msgHead = new MsgHead(cmdType, xmlString.length(), xmlString);
        sendMessage(socket, msgHead);
    }

    /**
     * 关闭视频流命令
     */
    private void closeMediaCmdRequest(Socket socket, int cmdType, CameraInfo cameraInfo)
            throws IOException {
        Request cmdRequest = new Request();
        cmdRequest.setType(cmdType);
        cmdRequest.setChannelid(cameraInfo.getChannelno());
        cmdRequest.setDevid(cameraInfo.getDeviceid());
        cmdRequest.setDevip(cameraInfo.getDeviceuserip());
        cmdRequest.setStreamtype(Command.STREAM_TYPE_MAIN);
        String xmlString = setRealTimeXmlBody(cmdRequest);
        LogUtils.e("关闭  " + cmdType + "\n" + xmlString);
        MsgHead msgHead = new MsgHead(cmdType, xmlString.length(), xmlString);
        sendMessage(socket, msgHead);
    }

    /**
     * 接收实时数据并进行配置
     */
    private void receiveLiveInitMsg(Socket socket) throws IOException, XmlPullParserException {
        LogUtils.e("接收消息");
        Response cmdResponse = receiveLiveMediaCmd(socket);
        StreamInfo streaminfo = cmdResponse.getStreamInfo();
        if (streaminfo == null) {
            throw new RuntimeException("VIDEO_GET_FAILED");
        }
        int videoWidth = streaminfo.getVideowidth();
        int videoHeight = streaminfo.getVideoheight();

        mCallBack.onSizeDefine(videoWidth, videoHeight);
        // 解码器
        createDecoder(streaminfo);
    }

    private void createDecoder(StreamInfo streaminfo)
            throws IOException {
        if (mDecoderWorker == null) {
            if (mSurface == null) {
                throw new NullPointerException("mSurface 不能为空");
            }
            mDecoderWorker = new DecoderWorker(mSurface, streaminfo);
        }
    }

    private void playVideo() throws Exception {
        LogUtils.e("开始连接数据流");
        mDataSocket = new Socket();
        mDataSocket.setSoTimeout(TIMEOUT);
        mDataSocket.connect(new InetSocketAddress(mCameraInfo.getForwarderserverip(),
                mCameraInfo.getForwarderserverdataport()), TIMEOUT);
        sendOpenMediaLiveRequest(mDataSocket, Command.CMD_MDU_STREAM_DESCRIBE, mCameraInfo);
        receiveMediaData(mDataSocket);
    }

    /**
     * @param socket  创建连接的socket
     * @param msgHead 消息头
     *                发送cmd命令消息
     */
    private void sendMessage(Socket socket, MsgHead msgHead)
            throws IOException {
        // 把发送消息放入wBuff中
        byte[] bXmlBody = msgHead.getXmlString().getBytes();
        ByteBuffer wBuff = ByteBuffer.allocate(MAX_LENGTH);
        wBuff.clear();
        wBuff.order(ByteOrder.BIG_ENDIAN);
        int cmdType = msgHead.getCmdType();
        int length = msgHead.getLength();
        wBuff.put(msgHead.getTag());
        wBuff.putInt(cmdType);
        wBuff.putInt(length);
        wBuff.put(bXmlBody);
        wBuff.flip();
        // 发送消息
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(wBuff.array());
        outputStream.flush();
        wBuff.clear();
    }

    /**
     * @param socket 读取数据的socket
     *               读取server端发回的消息, 接收cmd命令返回的初始化参数
     */
    private Response receiveLiveMediaCmd(Socket socket) throws IOException, XmlPullParserException {
        byte[] bytes = new byte[MAX_LENGTH];
        socket.getInputStream().read(bytes);
        ByteBuffer rBuff = ByteBuffer.wrap(bytes);
        rBuff.order(ByteOrder.BIG_ENDIAN);
        byte[] tag = new byte[4];
        rBuff.get(tag);
        int cmdType = rBuff.getInt();
        int length = rBuff.getInt();
        byte[] body = new byte[length];
        rBuff.get(body);
        String xmlBody = new String(body, "utf-8");
        LogUtils.e("接收\n" + xmlBody);
        Response cmdResponse = parseRealTimeXml(xmlBody);
        return cmdResponse;
    }

    /**
     * @param socket 接收数据socket
     *               接收数据通道相关信息
     */
    private void receiveMediaData(Socket socket) throws Exception {
        LogUtils.e(Thread.currentThread().getName() + " receiveMediaData" + socket);
        InputStream inputStream = socket.getInputStream();
        for (; ; ) {
            byte[] headByte = new byte[28];
            int headByteLen = 28;
            int headPosition = 0;
            long headReceiveLen = 0;
            //接收包体头信息
            while (headByteLen > 0) {
                headReceiveLen = inputStream.read(headByte, headPosition, headByteLen);
                //网络断开,通知界面
                if (headReceiveLen < 0) {
                    throw new SocketException("socket is close");
                }
                headByteLen -= headReceiveLen;
                headPosition += headReceiveLen;
            }
            ByteBuffer headByteBuffer = ByteBuffer.wrap(headByte);
            headByteBuffer.order(ByteOrder.BIG_ENDIAN);
            int nDeviceId = headByteBuffer.getInt();
            int nChannelId = headByteBuffer.getInt();
            int nStreamId = headByteBuffer.getInt();
            int nMediaType = headByteBuffer.getInt();
            // stream 包索引
            int nPacketIndex = headByteBuffer.getInt();
            int tTimeStamp = headByteBuffer.getInt();
            // 视频帧的长度，
            int bodyDataLen = headByteBuffer.getInt();
            //索引乱了或者字节长度为0
            if (nPacketIndex == 0xffff && bodyDataLen == 0) {
                continue;
            }
            long bodyReceiveLen;
            int bodyPosition = 0;
            byte[] bodyByte = new byte[bodyDataLen];
            while (bodyDataLen > 0) {
                bodyReceiveLen = inputStream.read(bodyByte, bodyPosition, bodyDataLen);
                //网络断开,通知界面
                if (bodyReceiveLen < 0) {
                    throw new SocketException("socket is close");
                }
                bodyDataLen -= bodyReceiveLen;
                bodyPosition += bodyReceiveLen;
            }
            mDecoderWorker.decode(bodyByte, bodyByte.length);
        }
    }

    /**
     * @param cmdRequest 请求的xml包体参数
     *                   封装xml包体
     */

    private String setRealTimeXmlBody(Request cmdRequest) {

        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "\n" +
                "<request>" +
                "\n" +
                "<type>" +
                cmdRequest.getType() +
                "</type>" +
                "\n" +
                "<devid>" +
                cmdRequest.getDevid() +
                "</devid>" +
                "\n" +
                "<devip>" +
                cmdRequest.getDevip() +
                "</devip>" +
                "\n" +
                "<channelid>" +
                cmdRequest.getChannelid() +
                "</channelid>" +
                "\n" +
                "<streamtype>" +
                cmdRequest.getStreamtype() +
                "</streamtype>" +
                "\n" +
                "<cameraid>" +
                cmdRequest.getCameraid() +
                "</cameraid>" +
                "\n" +
                "<taskid>" +
                cmdRequest.getTaskid() +
                "</taskid>" +
                "\n" +
                "<gatewaycode>" +
                cmdRequest.getGatewaycode() +
                "</gatewaycode>" +
                "\n" +
                "</request>";
    }

    /**
     * @param xmlBody 解析的字符串
     *                解析xml转换为response对象
     */
    private Response parseRealTimeXml(String xmlBody)
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
                        } else if ("streaminfo".equalsIgnoreCase(tag)) {
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
                    break;
                default:
            }
            evnType = xmlParse.next();
        }
        return response;
    }

    public void setCallBack(DataProcessCallBack callBack) {
        mCallBack = callBack;
    }

    public void setSurface(Surface surface) {
        mSurface = surface;
    }

    interface DataProcessCallBack {

        void onSizeDefine(int w, int h);

        void onByteBuff(byte[] w, int h);
    }

    private static class DecoderWorker {

        private MediaCodec mMediaCodec;

        DecoderWorker(Surface surface, StreamInfo streaminfo) throws IOException {
            mMediaCodec = MediaCodec.createDecoderByType("video/avc");
            MediaFormat mediaFormat = MediaFormat
                    .createVideoFormat("video/avc", streaminfo.getVideowidth(),
                            streaminfo.getVideoheight());
         /*
             //获取h264中的pps及sps数据
                if (UseSPSandPPS) {
                    byte[] header_sps = {0, 0, 0, 1, 103, 66, 0, 42, (byte) 149, (byte) 168, 30, 0, (byte) 137, (byte) 249, 102, (byte) 224, 32, 32, 32, 64};
                    byte[] header_pps = {0, 0, 0, 1, 104, (byte) 206, 60, (byte) 128, 0, 0, 0, 1, 6, (byte) 229, 1, (byte) 151, (byte) 128};
                    mediaformat.setByteBuffer("csd-0", ByteBuffer.wrap(header_sps));
                    mediaformat.setByteBuffer("csd-1", ByteBuffer.wrap(header_pps));
                }*/
            //设置帧率
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, streaminfo.getFramerate());
//            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, streaminfo.getbi);
//            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
//            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, debugger.getEncoderColorFormat());
            mMediaCodec.configure(mediaFormat, surface, null, 0);
            mMediaCodec.start();
        }

        public void release() {
            if (mMediaCodec != null) {
                mMediaCodec.release();
                mMediaCodec = null;
            }
        }

        public void decode(byte[] buf, int length) {
            // 这里解释一下  传0是不等待 传-1是一直等待 但是传-1会在很多机器上挂掉，所以还是用0吧 丢帧总比挂掉强
            int inputBufferIndex = mMediaCodec.dequeueInputBuffer(0);
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer;
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    // 从输入队列里去空闲buffer
                    inputBuffer = mMediaCodec.getInputBuffers()[inputBufferIndex];
                    inputBuffer.clear();
                } else {
                    // SDK_INT > LOLLIPOP
                    inputBuffer = mMediaCodec.getInputBuffer(inputBufferIndex);
                }
                inputBuffer.put(buf, 0, length);
                /*switch (buf[4] & 0x1f) {
                    case KEY_FRAME:
                        mMediaCodec.queueInputBuffer(inputBufferIndex, 0, length, 0L,
                                MediaCodec.BUFFER_FLAG_KEY_FRAME);
                        break;
                    case SPS_FRAME:
                    case PPS_FRAME:
                        mMediaCodec.queueInputBuffer(inputBufferIndex, 0, length, 0L,
                                MediaCodec.BUFFER_FLAG_CODEC_CONFIG);
                        break;
                    default:
                        mMediaCodec.queueInputBuffer(inputBufferIndex, 0, length, 0L, 0);
                        break;
                }*/
                mMediaCodec.queueInputBuffer(inputBufferIndex, 0, length, 0, 0);
            }
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 0);
            while (outputBufferIndex >= 0) {
                mMediaCodec.releaseOutputBuffer(outputBufferIndex, true);
                outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 0);
            }
        }
    }
}
