package sbingo.com.screenshotplus;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import sbingo.com.screenshotplus.tools.Constant;
import sbingo.com.screenshotplus.tools.ToastUtils;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    /**
     * 保存
     */
    private TextView mSave;
    /**
     * 裁剪区域
     */
    private LinearLayout mSaveArea;
    /**
     * 顶部裁剪坐标
     */
    private int mCutTop;
    /**
     * 左侧裁剪坐标
     */
    private int mCutLeft;
    /**
     * 截图成功后显示的控件
     */
    private ImageView mPicGet;
    private FrameLayout mFL;
    /**
     * 绘图区高度
     */
    private int mPicGetHeight;
    /**
     * 绘图区宽度
     */
    private int mPicGetWidth;
    /**
     * 最后的截图
     */
    private Bitmap saveBitmap;
    private FrameLayout mTotal;
    /**
     * 待裁剪区域的绝对坐标
     */
    private int[] mSavePositions = new int[2];
    /**
     * 成功动画handler
     */
    private SuccessHandler successHandler;
    /**
     * 恢复初始化handler
     */
    private InitHandler initHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {

        mSave = (TextView) findViewById(R.id.tv_save);
        mSaveArea = (LinearLayout) findViewById(R.id.ll_save_area);
        mPicGet = (ImageView) findViewById(R.id.iv_pic_get);
        mFL = (FrameLayout) findViewById(R.id.fl_pic);
        mTotal = (FrameLayout) findViewById(R.id.fl_total);

        mSave.setOnClickListener(this);

        successHandler = new SuccessHandler();
        initHandler = new InitHandler();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            mSaveArea.getLocationOnScreen(mSavePositions);
            mCutLeft = mSavePositions[0];
            mCutTop = mSavePositions[1];
            mPicGetHeight = mTotal.getHeight();
            mPicGetWidth = mTotal.getWidth();
        }
    }

    @Override
    public void onClick(View v) {
        mSave.setText("存储中……");
        mSave.setEnabled(false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                screenshot();
            }
        }).start();
    }

    private void screenshot() {
        // 获取屏幕
        View dView = getWindow().getDecorView();
        dView.setDrawingCacheEnabled(true);
        dView.buildDrawingCache();
        Bitmap bmp = dView.getDrawingCache();
        if (bmp != null) {
            try {
                //二次截图
                saveBitmap = Bitmap.createBitmap(mSaveArea.getWidth(), mSaveArea.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(saveBitmap);
                Paint paint = new Paint();
                canvas.drawBitmap(bmp, new Rect(mCutLeft, mCutTop, mCutLeft + mSaveArea.getWidth(), mCutTop + mSaveArea.getHeight()),
                        new Rect(0, 0, mSaveArea.getWidth(), mSaveArea.getHeight()), paint);

                File imageDir = new File(Constant.IMAGE_DIR);
                if (!imageDir.exists()) {
                    imageDir.mkdir();
                }
                String imageName = Constant.SCREEN_SHOT;
                File file = new File(imageDir, imageName);
                try {
                    if (file.exists()) {
                        file.delete();
                    }
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                FileOutputStream os = new FileOutputStream(file);
                saveBitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                os.flush();
                os.close();

                //将截图保存至相册并广播通知系统刷新
                MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(), imageName, null);
                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file));
                sendBroadcast(intent);

                successHandler.sendMessage(Message.obtain());

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            initHandler.sendMessage(Message.obtain());
        }

    }

    /**
     * 成功动画handler
     */
    private class SuccessHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            showSuccess();
        }
    }
    /**
     * 恢复初始化handler
     */
    private class InitHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            ToastUtils.show(MainActivity.this, "存储失败");
            mSave.setEnabled(true);
            mSave.setText("存储到相册");
        }
    }

    /**
     * 截图成功后显示动画
     */
    private void showSuccess() {
        ToastUtils.show(MainActivity.this, "保存成功");
        mPicGet.setImageBitmap(saveBitmap);
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(1f, 0.7f);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                ViewGroup.LayoutParams lp = mPicGet.getLayoutParams();
                lp.height = (int) (mPicGetHeight * (float) animation.getAnimatedValue());
                lp.width = (int) (mPicGetWidth * (float) animation.getAnimatedValue());
                mPicGet.setLayoutParams(lp);
            }
        });
        valueAnimator.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationStart(Animator animation) {
                mFL.setVisibility(View.VISIBLE);
                mPicGet.setVisibility(View.VISIBLE);
                ViewGroup.LayoutParams lp = mPicGet.getLayoutParams();
                lp.height = mPicGetHeight;
                lp.width = mPicGetWidth;
                mPicGet.setLayoutParams(lp);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mPicGet.setVisibility(View.GONE);
                        mFL.setVisibility(View.GONE);
                        mSave.setEnabled(true);
                        mSave.setText("存储到相册");
                    }
                }, 1500);
            }
        });
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.setDuration(800);
        valueAnimator.start();
    }

}

