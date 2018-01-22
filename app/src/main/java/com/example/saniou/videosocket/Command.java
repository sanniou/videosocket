package com.example.saniou.videosocket;

/**
 * Created songgx Ziv on 2016/8/17.
 * 请求视频的cmd命令集
 */
public class Command {

    public static final int STREAM_TYPE_CHILD = 0;//对应手机端的视频流,子码流
    public static final int STREAM_TYPE_MAIN = 1;//对应pc端的视频流，主码流
    /**
     * 转发服务器命令集
     */
    public static final int CMD_CMS_LOGIN_INFO = 0x1001;
    public static final int CMD_CMS_GET_DEVICE = 0x1002;
    public static final int CMD_CMS_ADD_DEVICE = 0x1003;
    public static final int CMD_CMS_DEL_DEVICE = 0x1004;
    public static final int CMD_CMS_GET_SERVICE = 0x1005;
    public static final int CMD_CMS_GET_ORGNATION = 0x1006;
    public static final int CMD_CMS_KEEP_ALIVE = 0x1007;

    public static final int CMD_MDU_STREAM_OPEN = 0x2001; //打开流命令
    public static final int CMD_MDU_STREAM_CLOSE = 0x2002; //关闭流命令

    public static final int CMD_MDU_STREAM_DESCRIBE = 0x2003; //打开视频流命令
    public static final int CMD_MDU_STREAM_UNDESCRIBE = 0x2004; //关闭视频流命令

    public static final int CMD_GET_STREAM_PARAM = 0x2005;

    /**
     * 存储服务命令集
     */

    public static final int CU_CMD_PLAY_BACK_SYNC_OPEN = 0x3001; // 客户端命令通道请求回放
    public static final int CU_CMD_PLAY_BACK_DESCRIBE = 0x3002;  // 客户端命令通道请求回放
//            #define  CU_CMD_PLAY_BACK_STOP                0x3003 // 客户端命令通道停止回放
//            #define  CU_CMD_PLAY_BACK_SPEED               0x3004  // 客户端命令通道设置回放速度
//            #define  CU_CMD_PLAY_BACK_REPOS                0x33005  // 客户端命令通道请求设置回放位置
//            #define  CU_DATA_PLAY_BACK_LOGIN               0x3006  // 客户端流回放通道注册
//            #define  CU_CMD_PLAY_BACK_PAUSE               0x3007
//            #define  CU_CMD_PLAY_BACK_RESTART             0x3008
//            #define  CU_CMD_PLAY_BACK_DOWNLOAD            0x3009
//            #define  CU_CMD_PLAY_BACK_END                 0x300a    ///// 终止回放
//            #define  CU_CMD_PLAY_BACK_FILEHEAD            0x300b
//            #define  CU_CMD_PLAY_BACK_FILEEND             0x300c
//            #define  CU_CMD_PLAY_BACK_FILEFRAME           0x300d
//            #define  CU_CMD_PLAY_BACK_DOWNLOAD_DESCRIBE   0x300e
//            #define  CU_CMD_PLAY_BACK_DOWNLOAD_END         0x3011   /// 服务器发给客户端
//            #define  CU_CMD_PLAY_BACK_DOWNLOAD_STOP         0x3012  //  客户端发给服务器
}
