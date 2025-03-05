package com.baidu.paddle.lite.demo.ppocr_demo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.paddle_lite.demo.common.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends Activity implements View.OnClickListener {
    private static final int REQUEST_CODE_SELECT_IMAGE = 1;
    private static final String TAG = "MainActivity";

    private ImageView imageView;
    private TextView tvStatus;
    private Button btnSelectImage;
    private Button btnDetect;

    private Bitmap selectedImage;
    private String savedImagePath;

    Native predictor = new Native();

    protected String detModelPath = "models/ch_PP-OCRv3_det_opt.nb";
    protected String recModelPath = "models/ch_PP-OCRv3_rec_opt.nb";
    protected String clsModelPath = "models/ch_ppocr_mobile_v2.0_cls_slim_opt.nb";
    protected String labelPath = "labels/ppocr_keys_v1.txt";
    protected String configPath = "config.txt";

    protected int cpuThreadNum = 1;
    protected String cpuPowerMode = "LITE_POWER_HIGH";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

        if (!checkAllPermissions()) {
            requestAllPermissions();
        }

        checkRun();
    }

    private void initView() {
        imageView = findViewById(R.id.imageView);
        tvStatus = findViewById(R.id.tv_status);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnDetect = findViewById(R.id.buttonDetect);

        if (btnSelectImage == null || btnDetect == null) {
            Log.e(TAG, "Các button không tồn tại. Kiểm tra layout file.");
            return;
        }

        btnSelectImage.setOnClickListener(this);
        btnDetect.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.btnSelectImage:
                selectImage();
                break;
            case R.id.buttonDetect:
                if (selectedImage != null) {
                    runDetection();
                } else {
                    Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        if(intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_CODE_SELECT_IMAGE);
        } else {
            Toast.makeText(this, "No application found to select images.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                InputStream imageStream = getContentResolver().openInputStream(imageUri);
                selectedImage = BitmapFactory.decodeStream(imageStream);
                imageView.setImageBitmap(selectedImage);
            } catch(FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(this, "Image not found", Toast.LENGTH_SHORT).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void runDetection() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        savedImagePath = Utils.getDCIMDirectory() + File.separator + dateFormat.format(new Date()) + ".png";
        Utils.saveBitmap(selectedImage, savedImagePath);
        Log.d(TAG, "Saved image path: " + savedImagePath);

        // Gọi hàm native để xử lý ảnh
        if (predictor.process(savedImagePath)) {
            Log.d(TAG, "Detection completed");
            Toast.makeText(this, "Detection completed", Toast.LENGTH_SHORT).show();
        } else {
            Log.e(TAG, "Detection failed");
            Toast.makeText(this, "Detection failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkRun() {
        try {
            copyAssets(this, labelPath);
            String labelRealDir = new File(getExternalFilesDir(null), labelPath).getAbsolutePath();
            Log.d(TAG, "Label path: " + labelRealDir + " exists: " + new File(labelRealDir).exists());

            copyAssets(this, configPath);
            String configRealDir = new File(getExternalFilesDir(null), configPath).getAbsolutePath();
            Log.d(TAG, "Config path: " + configRealDir + " exists: " + new File(configRealDir).exists());

            copyAssets(this, detModelPath);
            String detRealModelDir = new File(getExternalFilesDir(null), detModelPath).getAbsolutePath();
            Log.d(TAG, "Det model path: " + detRealModelDir + " exists: " + new File(detRealModelDir).exists());

            copyAssets(this, clsModelPath);
            String clsRealModelDir = new File(getExternalFilesDir(null), clsModelPath).getAbsolutePath();
            Log.d(TAG, "Cls model path: " + clsRealModelDir + " exists: " + new File(clsRealModelDir).exists());

            copyAssets(this, recModelPath);
            String recRealModelDir = new File(getExternalFilesDir(null), recModelPath).getAbsolutePath();
            Log.d(TAG, "Rec model path: " + recRealModelDir + " exists: " + new File(recRealModelDir).exists());

            boolean initResult = predictor.init(this,
                    detRealModelDir,
                    clsRealModelDir,
                    recRealModelDir,
                    configRealDir,
                    labelRealDir,
                    cpuThreadNum,
                    cpuPowerMode);
            if (initResult) {
                Log.d(TAG, "Predictor init succeeded");
            } else {
                Log.e(TAG, "Predictor init failed");
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception in checkRun: " + e.getMessage(), e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if(grantResults.length < 2 || grantResults[0] != PackageManager.PERMISSION_GRANTED ||
                grantResults[1] != PackageManager.PERMISSION_GRANTED) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Permission denied")
                    .setMessage("Please grant permissions via Settings.")
                    .setCancelable(false)
                    .setPositiveButton("Exit", (dialog, which) -> MainActivity.this.finish())
                    .show();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void requestAllPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, 0);
    }

    private boolean checkAllPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }
    public static void copyAssets(Context context, String assetName) throws IOException {
        AssetManager assetManager = context.getAssets();
        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(assetName);
            File outFile = new File(context.getExternalFilesDir(null), assetName);
            File outDir = outFile.getParentFile();
            if (!outDir.exists()) {
                boolean mkdirsResult = outDir.mkdirs();
                Log.d(TAG, "Tạo thư mục " + outDir.getAbsolutePath() + ": " + mkdirsResult);
            }
            out = new FileOutputStream(outFile);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
            Log.d(TAG, "Copy asset thành công: " + assetName);
        } catch (IOException e) {
            Log.e(TAG, "Failed to copy asset file: " + assetName, e);
            throw e;
        } finally {
            if (in != null) {
                try { in.close(); } catch (IOException e) { }
            }
            if (out != null) {
                try { out.close(); } catch (IOException e) { }
            }
        }
    }
}
