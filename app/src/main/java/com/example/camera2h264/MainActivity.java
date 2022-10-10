package com.example.camera2h264;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.graphics.ImageFormat;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import com.example.camera2h264.H264Encoder;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback,Camera.PreviewCallback,H264Encoder.EncoderListener{

    private static final String TAG = "dzp_test";
    private SurfaceHolder holder,holder_dec;
    private Camera camera;
    private SurfaceView surfaceView_cam, surfaceView_dec;
    private H264Encoder h264encoder = null;
    private FileOutputStream outputStream;
    private  H264Decoder h264decoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        checkPermission();
        //createFile();
    }

    private void createFile() {
//        File file = new File("/sdcard/test.h264");
        File file = new File(getExternalCacheDir(), "test.h264");
        try {
            outputStream = new FileOutputStream(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkPermission() {
        // 简单处理下权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED
        ) {
            String[] permissions = {
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA,
            };
            requestPermissions(permissions, 1);
        }
    }

    private void createFile(String path) {
//        File file = new File("/sdcard/test.h264");
        File file = new File(path);
        try {
            outputStream = new FileOutputStream(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initView() {
        surfaceView_cam = findViewById(R.id.cameraSurface);
        surfaceView_dec = findViewById(R.id.decodeSurface);
//        surfaceView_cam = findViewById(R.id.decodeSurface);
//        surfaceView_dec = findViewById(R.id.cameraSurface);

        holder = surfaceView_cam.getHolder();
        holder.addCallback(this);

        holder_dec = surfaceView_dec.getHolder();
        holder_dec.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                Log.e(TAG, "surfaceCreated: new H264Decoder");
                h264decoder = new H264Decoder(surfaceView_dec.getHolder().getSurface(), 1280, 720);
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                Log.e(TAG, "surfaceDestroyed: ================");
            }
        });
    }

    private void openCamera() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 0);
            return;
        }
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            return;
        }
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
            return;
        }

        for(int i = 0; i < Camera.getNumberOfCameras(); ++i) {
            Log.d(TAG, "openCamera: i:" + i);
            //createFile("/sdcard/DCIM/" + i + ".h264");
            //createFile();
        }
//        Log.d(TAG, "openCamera: Camera.getNumberOfCameras():" + Camera.getNumberOfCameras());
        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
//        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
        //获取相机参数
        Camera.Parameters parameters = camera.getParameters();
        //获取相机支持的预览的大小
        Camera.Size previewSize = getCameraPreviewSize(parameters);
//        int width = previewSize.width;
//        int height = previewSize.height;
        int width = 1280;
        int height = 720;
        //设置预览格式（也就是每一帧的视频格式）YUV420下的NV21
        parameters.setPreviewFormat(ImageFormat.NV21);
        //设置预览图像分辨率
        parameters.setPreviewSize(width, height);
        //设置预览图像帧率
        parameters.setPreviewFrameRate(15);
        //相机旋转0度
        camera.setDisplayOrientation(0);
        //配置camera参数
        camera.setParameters(parameters);
        try {
            camera.setPreviewDisplay(holder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "openCamera: width:" + width + "height:" + height);
        h264encoder = new H264Encoder(width, height);
        h264encoder.setEncoderListener(this);
//        h264decoder = new H264Decoder(surfaceView_dec.getHolder().getSurface(), width, height);
        //调用startPreview()用以更新preview的surface
        camera.startPreview();
        camera.setPreviewCallback(this);
    }

    private Camera.Size getCameraPreviewSize(Camera.Parameters parameters) {
        List<Camera.Size> list = parameters.getSupportedPreviewSizes();
        Camera.Size needSize = null;
        for (Camera.Size size : list) {
            if (needSize == null) {
                needSize = size;
                continue;
            }
            if (size.width >= needSize.width) {
                if (size.height > needSize.height) {
                    needSize = size;
                }
            }
        }
        return needSize;
    }

    public void releaseCamera(Camera camera) {
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
        }
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        openCamera();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        releaseCamera(camera);
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Log.d(TAG, "onPreviewFrame: data:" + data.length);
        h264encoder.encoderH264(data);
    }

    @Override
    public void h264(byte[] data) {
//        Log.d(TAG, "h264: h264 encode " + data.length);
//        try {
//            outputStream.write(data);
            Log.e(TAG, "bytes size " + data.length);
        h264decoder.decoderH264(data);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }
}