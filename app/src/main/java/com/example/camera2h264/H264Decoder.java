package com.example.camera2h264;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class H264Decoder {
    private static final String TAG = "dzp_test";
    //使用android MediaCodec解码
    private MediaCodec mediaCodec;
    private int width, height;
    private int frameRate = 15;
    Surface surface;
    private long mCount = 0;
//    ByteBuffer inputBuffers;


    H264Decoder(Surface surface, int width, int height) {
        this.width = width;
        this.height = height;
        this.surface = surface;
        initMediaCodec();
    }

    private void initMediaCodec() {
        try {
            //创建解码器 H264的Type为  AAC
//            mediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            mediaCodec = MediaCodec.createByCodecName("c2.rk.avc.decoder"); // 指定使用rk解码器
            //创建配置
//            MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", 540, 960);
            MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", width, height);
            //设置解码预期的帧速率【以帧/秒为单位的视频格式的帧速率的键】
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
            //配置绑定mediaFormat和surface
            mediaCodec.configure(mediaFormat, surface, null, 0);
            //开始解码
            mediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
            //创建解码失败
            Log.e(TAG, "创建解码失败");
        }
    }

    public void decoderH264(byte[] data) {

//        Log.e(TAG, "decode: " + data.length);
        ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
        int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);//获取输入缓冲区的索引
        Log.d(TAG, "dequeueInputBuffer :" + inputBufferIndex);
        if (inputBufferIndex >= 0) {

//            Log.e(TAG, "decode: 000000000000000000000000");
//            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
//            inputBuffer.put(byteBuffer);//先获取缓冲区，再放入值
            inputBuffer.put(data);//先获取缓冲区，再放入值
//            mediaCodec.queueInputBuffer(inputBufferIndex, 0, data.length,  mCount * 1000000 / 30, 0);//四个参数，第一个是输入缓冲区的索引，第二个是放入的数据大小，第三个是时间戳，保证递增就是
            mediaCodec.queueInputBuffer(inputBufferIndex, 0, data.length,  mCount, 0);//四个参数，第一个是输入缓冲区的索引，第二个是放入的数据大小，第三个是时间戳，保证递增就是
            mCount++;
        }
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
        while (outputBufferIndex >= 0) {
//            Log.e(TAG, "decode: 1111111111111111111111111111111");
            mediaCodec.releaseOutputBuffer(outputBufferIndex, true);//释放缓冲区解码的数据到surfaceview，一般到了这一步，surfaceview上就有画面了
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
        }
    }

}
