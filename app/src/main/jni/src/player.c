//
// Created by Administrator on 2016/9/18.
//
#include <stdio.h>
#include <time.h>
#include <android/native_window_jni.h>
#include "SDL.h"
#include "SDL_main.h"
#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"
#include "libswscale/swscale.h"
#include "libavutil/log.h"

#include <jni.h>
#include <android/log.h>

#ifdef __ANDROID__
#define LOGE(format, ...)  __android_log_print(ANDROID_LOG_ERROR, "(>_<)", format, ##__VA_ARGS__)
#define LOGI(format, ...)  __android_log_print(ANDROID_LOG_INFO,  "(=_=)", format, ##__VA_ARGS__)
#else
#define LOGE(format, ...)  printf("(>_<) " format "\n", ##__VA_ARGS__)
#define LOGI(format, ...)  printf("(^_^) " format "\n", ##__VA_ARGS__)
#endif

extern void SDL_Android_Init(JNIEnv *env, jclass cls);

AVCodecContext *pCodecCtx = NULL;
AVCodec *pCodec = NULL;
AVFrame *pFrame = NULL;
AVFrame *pFrameYUV = NULL;
uint8_t *out_buffer = NULL;
SDL_Texture *bmp = NULL;
SDL_Window *screen = NULL;
SDL_Renderer *renderer = NULL;

SDL_Rect rect;
SDL_Event event;
static struct SwsContext *img_convert_ctx = NULL;


int ret = 0;
int got_picture = 0;
int nInit = 0;

void InitDecoder(int nWidth, int nHeight);

void Video_MediaDecode_decode(uint8_t *byteArray, int nSize);

void Java_com_example_saniou_videosocket_DataProcess_init(JNIEnv *env, jclass cls, jint nWidth, jint nHeight) {
    av_register_all();
    LOGI("av_register_all");
    SDL_DestroyTexture(bmp);
    LOGE("SDL_DestroyTexture ===== \n");
    SDL_DestroyRenderer(renderer);
    LOGE("SDL_DestroyRenderer ===== \n");
    SDL_DestroyWindow(screen);
    LOGE("SDL_DestroyWindow ===== \n");
    av_free(pFrameYUV);
    LOGE("av_free(pFrameYUV) ===== \n");
    sws_freeContext(img_convert_ctx);
    LOGE("sws_freeContext ===== \n");
    avcodec_close(pCodecCtx);
    LOGE("avcodec_close(pCodecCtx) ===== \n");
    av_free(out_buffer);
    LOGE("av_free(out_buffer) ===== \n");
    InitDecoder(nWidth, nHeight);
    SDL_Android_Init(env, cls);
    SDL_SetMainReady();
    if (SDL_Init(SDL_INIT_VIDEO | SDL_INIT_AUDIO | SDL_INIT_TIMER)) {
        LOGE("Could not initialize SDL - %s. \n", SDL_GetError());
        exit(1);
    }
    screen = NULL;
    renderer = NULL;
    bmp = NULL;
}

void Java_com_example_saniou_videosocket_DataProcess_decode(JNIEnv *env, jclass cls,
                                                                jbyteArray byteArray, jint nSize) {
    got_picture = 0;
    AVPacket packet;
    av_init_packet(&packet);

    uint8_t *data = (*env)->GetByteArrayElements(env, byteArray, 0);
    LOGI("**********  buffer info %d %d %d %d %d ********** \n", data[0], data[1], data[2], data[3],
         data[4]);
    packet.data = data;
    packet.size = nSize;
    pFrame = av_frame_alloc();

    if (pCodecCtx == NULL) {
        LOGI("**********  pCodecCtx == NULL **********");
    }
    if (pFrame == NULL) {
        LOGI("**********  pFrame == NULL **********");
    }

    int len = avcodec_decode_video2(pCodecCtx, pFrame, &got_picture, &packet);

    if (len < 0) {
        LOGI("**********   decoder fail **********");
        //  return;
    }

    if (got_picture) {
        if (screen == NULL) {
            LOGI("screen w1=%d,h1=%d", pCodecCtx->width, pCodecCtx->height);
            screen = SDL_CreateWindow("My Player Window", SDL_WINDOWPOS_UNDEFINED,
                                      SDL_WINDOWPOS_UNDEFINED,
                                      pCodecCtx->width, pCodecCtx->height, SDL_WINDOW_OPENGL);
            LOGE("screen w1=%d,h1=%d", pCodecCtx->width, pCodecCtx->height);
        }
        if (screen == NULL) {
            LOGI("screen  == NULL \n");
        }

        if (renderer == NULL) {
            renderer = SDL_CreateRenderer(screen, -1, 0);
        }
        if (renderer == NULL) {
            LOGI("renderer  == NULL \n");
        }
        if (bmp == NULL) {
            bmp = SDL_CreateTexture(renderer, SDL_PIXELFORMAT_IYUV, SDL_TEXTUREACCESS_STREAMING,
                                    pCodecCtx->width, pCodecCtx->height);
        }

        if (bmp == NULL) {
            LOGI(" bmp == NULL");
        }

        LOGI("got_picture  w1=%d,h1=%d", pCodecCtx->width, pCodecCtx->height);
        sws_scale(img_convert_ctx, (const uint8_t *const *) pFrame->data,
                  pFrame->linesize, 0,
                  pCodecCtx->height, pFrameYUV->data, pFrameYUV->linesize);
        SDL_UpdateTexture(bmp, NULL, pFrameYUV->data[0], pFrameYUV->linesize[0]);
        SDL_RenderClear(renderer);
        SDL_RenderCopy(renderer, bmp, NULL, NULL);
        SDL_RenderPresent(renderer);
    }
    av_frame_free(&pFrame);
    av_free_packet(&packet);
}

void Java_com_example_saniou_videosocket_DataProcess_destroy(JNIEnv *env, jclass cls) {
    SDL_PollEvent(&event);
    switch (event.type) {
        case SDL_QUIT: {
            SDL_Quit();
            exit(0);
            LOGE("exit(0) \n");
            break;
        }
        default:
            break;
    }
    SDL_DestroyTexture(bmp);
    LOGE("SDL_DestroyTexture ===== \n");
    SDL_DestroyRenderer(renderer);
    LOGE("SDL_DestroyRenderer ===== \n");
    SDL_DestroyWindow(screen);
    LOGE("SDL_DestroyWindow ===== \n");
    av_free(pFrameYUV);
    LOGE("av_free(pFrameYUV) ===== \n");
    sws_freeContext(img_convert_ctx);
    LOGE("sws_freeContext ===== \n");
    avcodec_close(pCodecCtx);
    LOGE("avcodec_close(pCodecCtx) ===== \n");
    av_free(pFrameYUV);
    LOGE("av_free(pFrameYUV) ===== \n");
    av_free(out_buffer);
    LOGE("av_free(out_buffer) ===== \n");

}

void InitDecoder(int nWidth, int nHeight) {
    pCodec = avcodec_find_decoder(AV_CODEC_ID_H264);
    if (pCodec == NULL) {
        LOGE("Codec not found.\n");
        return;
    }
    pCodecCtx = avcodec_alloc_context3(pCodec);
    if (pCodecCtx == NULL) {
        LOGE("Codectext not found.\n");
        return;
    }
    pCodecCtx->codec_id = pCodec->id;
    pCodecCtx->bit_rate = 100000;
    pCodecCtx->width = nWidth;
    pCodecCtx->height = nHeight;
    pCodecCtx->time_base.num = 1;
    pCodecCtx->time_base.den = 25;
    pCodecCtx->gop_size = 25;
    pCodecCtx->max_b_frames = 1;
    pCodecCtx->pix_fmt = AV_PIX_FMT_YUV420P;
    pCodecCtx->flags |= CODEC_FLAG_LOW_DELAY;
    pCodecCtx->max_b_frames = 0;
    pCodecCtx->refs = 1;
    pCodecCtx->dct_algo = 0;
    pCodecCtx->me_pre_cmp = 2;
    pCodecCtx->me_method = 7;
    pCodecCtx->qmin = 3;
    pCodecCtx->qmax = 31;
    pCodecCtx->max_qdiff = 3;
    pCodecCtx->qcompress = 0.5;
    pCodecCtx->qblur = 0.5;
    pCodecCtx->nsse_weight = 8;
    pCodecCtx->i_quant_factor = (float) 0.8;
    pCodecCtx->b_quant_factor = 1.25;
    pCodecCtx->b_quant_offset = 1.25;
    pCodecCtx->thread_count = 1;
    pCodecCtx->rc_buffer_size = 0;
    pCodecCtx->rc_initial_buffer_occupancy = 0;

    if (avcodec_open2(pCodecCtx, pCodec, NULL) < 0) {
        LOGE("Could not open codec.\n");
        return;
    }

    pFrameYUV = av_frame_alloc();
    img_convert_ctx = sws_getContext(pCodecCtx->width, pCodecCtx->height,
                                     AV_PIX_FMT_YUV420P, pCodecCtx->width, pCodecCtx->height,
                                     AV_PIX_FMT_YUV420P, SWS_BICUBIC, NULL, NULL, NULL);
    LOGE("sws_getContext.\n");
    int numBytes = avpicture_get_size(AV_PIX_FMT_YUV420P, pCodecCtx->width,
                                      pCodecCtx->height);
    out_buffer = (uint8_t *) av_malloc(numBytes * sizeof(uint8_t));

    avpicture_fill((AVPicture *) pFrameYUV, out_buffer, AV_PIX_FMT_YUV420P,
                   pCodecCtx->width, pCodecCtx->height);
    LOGE("avpicture_fill.\n");

}


