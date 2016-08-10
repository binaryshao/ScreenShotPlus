package sbingo.com.screenshotplus.tools;

import android.os.Environment;

import java.io.File;

/**
 * 常量类
 */
public class Constant {
    public static final String IMAGE_DIR = Environment.getExternalStorageDirectory() + File.separator + "Android截屏";
    public static final String SCREEN_SHOT ="screenshot.png";
}
