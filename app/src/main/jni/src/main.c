/*
 * main.c
 *
 *  Created on: 2015-5-8
 *      Author: ming_xu
 */

#ifdef __ANDROID__

#include <jni.h>
#include <android/native_window_jni.h>
#include "SDL.h"
#include "SDL_thread.h"
#include "SDL_events.h"
#include "../include/logger.h"
#include "../ffmpeg/include/libavcodec/avcodec.h"
#include "../ffmpeg/include/libavformat/avformat.h"
#include "../ffmpeg/include/libavutil/pixfmt.h"
#include "../ffmpeg/include/libswscale/swscale.h"

#define OUTPUT_YUV420P 0


int main(int argc, char *argv[]) {
    char *file_path = argv[1];
    LOGI("file_path:%s", file_path);

//  AVFormatContext *pFormatCtx;
//	int             i, videoindex;
//	AVCodecContext  *pCodecCtx;
//	AVCodec         *pCodec;
//	AVFrame *pFrame,*pFrameYUV;
//	uint8_t *out_buffer;
//	AVPacket *packet;
//	int y_size;
//	int ret, got_picture;
//	struct SwsContext *img_convert_ctx;
//
//	//SDL---------------------------
//	int screen_w=0,screen_h=0;
//	SDL_Window *screen;
//	SDL_Renderer* sdlRenderer;
//	SDL_Texture* sdlTexture;
//	SDL_Rect sdlRect;
//
//	FILE *fp_yuv;
//
//	av_register_all();
//	avformat_network_init();
//	pFormatCtx = avformat_alloc_context();
//
//	if(avformat_open_input(&pFormatCtx,file_path,NULL,NULL)!=0){
//		printf("Couldn't open input stream.\n");
//		return -1;
//	}
//	if(avformat_find_stream_info(pFormatCtx,NULL)<0){
//		printf("Couldn't find stream information.\n");
//		return -1;
//	}
//	videoindex=-1;
//	for(i=0; i<pFormatCtx->nb_streams; i++)
//		if(pFormatCtx->streams[i]->codec->codec_type==AVMEDIA_TYPE_VIDEO){
//			videoindex=i;
//			break;
//		}
//	if(videoindex==-1){
//		printf("Didn't find a video stream.\n");
//		return -1;
//	}
//	pCodecCtx=pFormatCtx->streams[videoindex]->codec;
//	pCodec=avcodec_find_decoder(pCodecCtx->codec_id);
//	if(pCodec==NULL){
//		printf("Codec not found.\n");
//		return -1;
//	}
//	if(avcodec_open2(pCodecCtx, pCodec,NULL)<0){
//		printf("Could not open codec.\n");
//		return -1;
//	}
//
//	pFrame=av_frame_alloc();
//	pFrameYUV=av_frame_alloc();
//	out_buffer=(uint8_t *)av_malloc(avpicture_get_size(AV_PIX_FMT_YUV420P, pCodecCtx->width, pCodecCtx->height));
//	avpicture_fill((AVPicture *)pFrameYUV, out_buffer, AV_PIX_FMT_YUV420P, pCodecCtx->width, pCodecCtx->height);
//	packet=(AVPacket *)av_malloc(sizeof(AVPacket));
//	//Output Info-----------------------------
//	printf("--------------- File Information ----------------\n");
//	av_dump_format(pFormatCtx,0,file_path,0);
//	printf("-------------------------------------------------\n");
//	img_convert_ctx = sws_getContext(pCodecCtx->width, pCodecCtx->height, pCodecCtx->pix_fmt,
//		pCodecCtx->width, pCodecCtx->height, AV_PIX_FMT_YUV420P, SWS_BICUBIC, NULL, NULL, NULL);
//
//#if OUTPUT_YUV420P
//	fp_yuv=fopen("output.yuv","wb+");
//#endif
//
//	if(SDL_Init(SDL_INIT_VIDEO | SDL_INIT_AUDIO | SDL_INIT_TIMER)) {
//		printf( "Could not initialize SDL - %s\n", SDL_GetError());
//		return -1;
//	}
//
//	screen_w = pCodecCtx->width;
//	screen_h = pCodecCtx->height;
//	//SDL 2.0 Support for multiple windows
//	screen = SDL_CreateWindow("Simplest ffmpeg player's Window", SDL_WINDOWPOS_UNDEFINED, SDL_WINDOWPOS_UNDEFINED,
//		screen_w, screen_h,
//		SDL_WINDOW_OPENGL);
//
//	if(!screen) {
//		printf("SDL: could not create window - exiting:%s\n",SDL_GetError());
//		return -1;
//	}
//
//	sdlRenderer = SDL_CreateRenderer(screen, -1, 0);
//	//IYUV: Y + U + V  (3 planes)
//	//YV12: Y + V + U  (3 planes)
//	sdlTexture = SDL_CreateTexture(sdlRenderer, SDL_PIXELFORMAT_IYUV, SDL_TEXTUREACCESS_STREAMING,pCodecCtx->width,pCodecCtx->height);
//
//	sdlRect.x=0;
//	sdlRect.y=0;
//	sdlRect.w=screen_w;
//	sdlRect.h=screen_h;
//
//	//SDL End----------------------
//	while(av_read_frame(pFormatCtx, packet)>=0){
//		if(packet->stream_index==videoindex){
//			ret = avcodec_decode_video2(pCodecCtx, pFrame, &got_picture, packet);
//			if(ret < 0){
//				printf("Decode Error.\n");
//				return -1;
//			}
//			if(got_picture){
//				sws_scale(img_convert_ctx, (const uint8_t* const*)pFrame->data, pFrame->linesize, 0, pCodecCtx->height,
//					pFrameYUV->data, pFrameYUV->linesize);
//
//#if OUTPUT_YUV420P
//				y_size=pCodecCtx->width*pCodecCtx->height;
//				fwrite(pFrameYUV->data[0],1,y_size,fp_yuv);    //Y
//				fwrite(pFrameYUV->data[1],1,y_size/4,fp_yuv);  //U
//				fwrite(pFrameYUV->data[2],1,y_size/4,fp_yuv);  //V
//#endif
//				//SDL---------------------------
//#if 0
//				SDL_UpdateTexture( sdlTexture, NULL, pFrameYUV->data[0], pFrameYUV->linesize[0] );
//#else
//				SDL_UpdateYUVTexture(sdlTexture, &sdlRect,
//				pFrameYUV->data[0], pFrameYUV->linesize[0],
//				pFrameYUV->data[1], pFrameYUV->linesize[1],
//				pFrameYUV->data[2], pFrameYUV->linesize[2]);
//#endif
//
//				SDL_RenderClear( sdlRenderer );
//				SDL_RenderCopy( sdlRenderer, sdlTexture,  NULL, &sdlRect);
//				SDL_RenderPresent( sdlRenderer );
//				//SDL End-----------------------
//				//Delay 40ms
//				SDL_Delay(40);
//			}
//		}
//		av_free_packet(packet);
//	}
//	//flush decoder
//	//FIX: Flush Frames remained in Codec
//	while (1) {
//		ret = avcodec_decode_video2(pCodecCtx, pFrame, &got_picture, packet);
//		if (ret < 0)
//			break;
//		if (!got_picture)
//			break;
//		sws_scale(img_convert_ctx, (const uint8_t* const*)pFrame->data, pFrame->linesize, 0, pCodecCtx->height,
//			pFrameYUV->data, pFrameYUV->linesize);
//#if OUTPUT_YUV420P
//		int y_size=pCodecCtx->width*pCodecCtx->height;
//		fwrite(pFrameYUV->data[0],1,y_size,fp_yuv);    //Y
//		fwrite(pFrameYUV->data[1],1,y_size/4,fp_yuv);  //U
//		fwrite(pFrameYUV->data[2],1,y_size/4,fp_yuv);  //V
//#endif
//		//SDL---------------------------
//		SDL_UpdateTexture( sdlTexture, &sdlRect, pFrameYUV->data[0], pFrameYUV->linesize[0] );
//		SDL_RenderClear( sdlRenderer );
//		SDL_RenderCopy( sdlRenderer, sdlTexture,  NULL, &sdlRect);
//		SDL_RenderPresent( sdlRenderer );
//		//SDL End-----------------------
//		//Delay 40ms
//		SDL_Delay(40);
//	}
//
//	sws_freeContext(img_convert_ctx);
//
//#if OUTPUT_YUV420P
//	fclose(fp_yuv);
//#endif
//
//	SDL_Quit();
//
//	av_frame_free(&pFrameYUV);
//	av_frame_free(&pFrame);
//	avcodec_close(pCodecCtx);
//	avformat_close_input(&pFormatCtx);
//
//	return 0;



    AVFormatContext *pFormatCtx;
    AVCodecContext *pCodecCtx;
    AVCodec *pCodec;
    AVFrame *pFrame, *pFrameYUV;
    AVPacket *packet;
    uint8_t *out_buffer;

    SDL_Texture *bmp = NULL;
    SDL_Window *screen = NULL;
    SDL_Rect rect;
    SDL_Event event;

    FILE *fp_yuv;

    static struct SwsContext *img_convert_ctx;

    int videoStream, i, numBytes;
    int ret, got_picture;

    av_register_all();
    pFormatCtx = avformat_alloc_context();

    if (SDL_Init(SDL_INIT_VIDEO | SDL_INIT_AUDIO | SDL_INIT_TIMER)) {
        LOGE("Could not initialize SDL - %s. \n", SDL_GetError());
        exit(1);
    }

    if (avformat_open_input(&pFormatCtx, file_path, NULL, NULL) != 0) {
        LOGE("can't open the file. \n");
        return -1;
    }

    if (avformat_find_stream_info(pFormatCtx, NULL) < 0) {
        LOGE("Could't find stream infomation.\n");
        return -1;
    }

    videoStream = 1;
    for (i = 0; i < pFormatCtx->nb_streams; i++) {
        if (pFormatCtx->streams[i]->codec->codec_type == AVMEDIA_TYPE_VIDEO) {
            videoStream = i;
        }
    }

    LOGI("videoStream:%d", videoStream);
    if (videoStream == -1) {
        LOGE("Didn't find a video stream.\n");
        return -1;
    }

    pCodecCtx = pFormatCtx->streams[videoStream]->codec;

    pCodec = avcodec_find_decoder(pCodecCtx->codec_id);

    if (pCodec == NULL) {
        LOGE("Codec not found.\n");
        return -1;
    }

    if (avcodec_open2(pCodecCtx, pCodec, NULL) < 0) {
        LOGE("Could not open codec.\n");
        return -1;
    }

    pFrame = av_frame_alloc();
    pFrameYUV = av_frame_alloc();

#if OUTPUT_YUV420P
	fp_yuv=fopen("mnt/sdcard/output.yuv","wb+");
#endif

    //---------------------------init sdl---------------------------//
    screen = SDL_CreateWindow("My Player Window", SDL_WINDOWPOS_UNDEFINED,
    		SDL_WINDOWPOS_UNDEFINED, pCodecCtx->width, pCodecCtx->height,
            SDL_WINDOW_OPENGL);

    LOGI("w1=%d,h1=%d",pCodecCtx->width,pCodecCtx->height);

    SDL_Renderer *renderer = SDL_CreateRenderer(screen, -1, 0);

    bmp = SDL_CreateTexture(renderer, SDL_PIXELFORMAT_IYUV,
            SDL_TEXTUREACCESS_STREAMING, pCodecCtx->width, pCodecCtx->height);

    //-------------------------------------------------------------//

    img_convert_ctx = sws_getContext(pCodecCtx->width, pCodecCtx->height,
            pCodecCtx->pix_fmt, pCodecCtx->width, pCodecCtx->height,
            AV_PIX_FMT_YUV420P, SWS_BICUBIC, NULL, NULL, NULL);

    numBytes = avpicture_get_size(AV_PIX_FMT_YUV420P, pCodecCtx->width,
            pCodecCtx->height);
    out_buffer = (uint8_t *) av_malloc(numBytes * sizeof(uint8_t));
    avpicture_fill((AVPicture *) pFrameYUV, out_buffer, AV_PIX_FMT_YUV420P,
            pCodecCtx->width, pCodecCtx->height);

    rect.x = 0;
    rect.y = 0;
    rect.w = pCodecCtx->width;
    rect.h = pCodecCtx->height;

    LOGI("w2=%d,h2=%d",pCodecCtx->width,pCodecCtx->height);

    int y_size = pCodecCtx->width * pCodecCtx->height;

    packet = (AVPacket *) malloc(sizeof(AVPacket));
    av_new_packet(packet, y_size);

    av_dump_format(pFormatCtx, 0, file_path, 0);

    while (av_read_frame(pFormatCtx, packet) >= 0) {
//#if OUTPUT_YUV420P
//    	fwrite(packet->data, 1, packet->size, fp_yuv);
//#endif
        if (packet->stream_index == videoStream) {
            ret = avcodec_decode_video2(pCodecCtx, pFrame, &got_picture,
                    packet);
            LOGI("pFrame.format=%d,pFrame.flags=%d",pFrame->format,pFrame->flags);
            if (ret < 0) {
                LOGE("decode error.\n");
                return -1;
            }

            LOGI("got_picture:%d", got_picture);
            if (got_picture) {
                sws_scale(img_convert_ctx,
                        ( const uint8_t * const *) pFrame->data,
                        pFrame->linesize, 0, pCodecCtx->height, pFrameYUV->data,
                        pFrameYUV->linesize);
#if OUTPUT_YUV420P
		int y_size=pCodecCtx->width*pCodecCtx->height;
		fwrite(pFrameYUV->data[0],1,y_size,fp_yuv);    //Y
		fwrite(pFrameYUV->data[1],1,y_size/4,fp_yuv);  //U
		fwrite(pFrameYUV->data[2],1,y_size/4,fp_yuv);  //V
#endif
                ////iPitch 计算yuv一行数据占的字节数
                SDL_UpdateTexture(bmp, NULL, pFrameYUV->data[0], pFrameYUV->linesize[0]);
                SDL_RenderClear(renderer);
                SDL_RenderCopy(renderer, bmp, NULL, NULL);
                SDL_RenderPresent(renderer);
            }
            SDL_Delay(20);
        }
        av_free_packet(packet);

        SDL_PollEvent(&event);
        switch (event.type) {
        case SDL_QUIT:
            SDL_Quit();
            exit(0);
            break;
        default:
            break;
        }
    }
    SDL_DestroyTexture(bmp);

#if OUTPUT_YUV420P
	fclose(fp_yuv);
#endif

    av_free(out_buffer);
    av_free(pFrameYUV);
    avcodec_close(pCodecCtx);
    avformat_close_input(&pFormatCtx);

    return 0;
}


#endif /* __ANDROID__ */


