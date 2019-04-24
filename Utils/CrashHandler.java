

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Process;
import android.util.Log;


import com.booslink.sdm_midl_launcher.constant.Constant;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.os.Environment.DIRECTORY_DOWNLOADS;


public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String TAG = "CrashHandler";
    private static final boolean DEBUG = true;

    private static final String PATH = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS)
            .getPath() + "/"+ Constant.PKG_NAME + "/log/";
    private static final String FILE_NAME = "crash";
    private static final String FILE_NAME_SUFFIX = ".trace";

    private static CrashHandler sInstance = new CrashHandler();
    private Thread.UncaughtExceptionHandler mDefaultCrashHandler;
    private Context mContext;

    private CrashHandler() {

    }

    public static CrashHandler getInstance() {
        return sInstance;
    }

    public void init(Context context) {
        mDefaultCrashHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
        mContext = context.getApplicationContext();
    }

    /**
     * 未被捕获的异常，系统会自动调用该方法
     *
     * @param t  出现未捕获异常的线程
     * @param ex 未捕获的异常
     */
    @Override
    public void uncaughtException(Thread t, Throwable ex) {
        //导出异常信息到SD卡中
        dumpExceptionToSDCard(ex);
        //上传异常信息到服务器
        //uploadExceptionToServer();

        ex.printStackTrace();
        //如果系统提供了默认的异常处理器，则交给系统区结束程序，否则就由自己结束自己
        if (mDefaultCrashHandler != null) {
            mDefaultCrashHandler.uncaughtException(t, ex);
        } else {
            Process.killProcess(Process.myPid());
        }
    }

    /**
     * @param e
     */
    private void dumpExceptionToSDCard(Throwable e) {
        //如果sd卡不存在或无法使用，则无法把异常信息写入到sd中
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            if (DEBUG) {
                Log.w(TAG, "sdcard unmounted,skip dump exception");
                return;
            }
        }

        File dir = new File(PATH);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        long current = System.currentTimeMillis();
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(current));
        File file = new File(PATH + FILE_NAME + time + FILE_NAME_SUFFIX);
        try {
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));
            pw.println(time);
            dumpPhoneInfo(pw);
            pw.println();
            e.printStackTrace(pw);
            pw.close();
        } catch (Exception e1) {
            Log.e(TAG, "dump crash info failed");
        }
    }

    /**
     * exception upload to server
     */
    private void uploadExceptionToServer() {
    }

    private void dumpPhoneInfo(PrintWriter pw) throws PackageManager.NameNotFoundException {
        PackageManager pm = mContext.getPackageManager();
        PackageInfo pi = pm.getPackageInfo(mContext.getPackageName(), PackageManager.GET_ACTIVITIES);
        pw.print("App VersionName：");
        pw.println(pi.versionName);
        pw.print("App VersionCode：");
        pw.println(pi.versionCode);

        //Android版本号
        pw.print("OS Version is:");
        pw.print(Build.VERSION.RELEASE);
        pw.print("_");
        pw.println(Build.VERSION.SDK_INT);

        //制造商
        pw.print("Vendor：");
        pw.println(Build.MANUFACTURER);

        //型号
        pw.print("Model：");
        pw.println(Build.MODEL);

        //CPU架构
        pw.print("CPU ABI：");
        pw.println(Build.CPU_ABI);
    }
}
