package com.zlb.base;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.multidex.MultiDex;
import com.alibaba.android.arouter.launcher.ARouter;
import com.kingja.loadsir.core.LoadSir;
import com.squareup.leakcanary.LeakCanary;
import com.tencent.bugly.crashreport.CrashReport;
import com.zlb.commontips.CustomCallback;
import com.zlb.commontips.EmptyCallback;
import com.zlb.commontips.ErrorCallback;
import com.zlb.commontips.LoadingCallback;
import com.zlb.commontips.TimeoutCallback;
import com.zlb.utils.ntp.SyncNtpTimeUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;
import javax.inject.Inject;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasAndroidInjector;

/**
 * Core，核心就是
 *
 * May you do good and not evil
 * May you find forgiveness for yourself and forgive others
 * May you share freely,never taking more than you give
 *
 */
public abstract class BaseApplication extends Application implements HasAndroidInjector {
    public boolean isDebug = false;  //App 是否是调试模式，默认不是，不要把调试信息加进去

    @Inject
    DispatchingAndroidInjector<Object> dispatchingAndroidInjector;

    @Override
    public AndroidInjector<Object> androidInjector() {
        return dispatchingAndroidInjector;
    }


    private static Context appContext;

    @Inject
    SyncNtpTimeUtils syncNTPTime; //NTP 时间校准

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();

        initARouter();

        initDI();

//        //内存泄漏检查
//        initLeakCanary();

        //时间校准
        syncNTPTime.syncNTPTime();

        //UI 状态提示页面，全局的，不用每个页面都添加
        LoadSir.beginBuilder()
                .addCallback(new ErrorCallback())
                .addCallback(new EmptyCallback())
                .addCallback(new LoadingCallback())
                .addCallback(new TimeoutCallback())
                .addCallback(new CustomCallback())
                .setDefaultCallback(LoadingCallback.class)
                .commit();

        //测试环境下数据库可视化调试
        showDebugDBAddressLogToast(this);

        CrashReport.initCrashReport(getApplicationContext(), "900025400", false);

    }

    private void initDI() {
        injectApp();
    }

    /**
     * 这是类库底层的injectApp代码示例，你应该在你的Module中重写该方法
     */
    abstract protected void injectApp();

    public static Context getAppContext() {
        return appContext;
    }

    @Override
    public void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(base);
    }

    /**
     * 初始化内存泄露检测
     */
    private void initLeakCanary() {
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
    }

    /**
     * ARouter 尽早的初始化
     */
    private void initARouter() {
        //ARouter 相关的配置
        if (isAppDebug()) {          // 这两行必须写在init之前，否则这些配置在init过程中将无效
            ARouter.openLog();       // 打印日志
            ARouter.openDebug();     // 开启调试模式(如果在InstantRun模式下运行，必须开启调试模式！线上版本需要关闭,否则有安全风险)
        }
        ARouter.init(this);          // 尽可能早，推荐在Application中初始化
    }

    /**
     * 判断App是否是Debug版本.
     *
     * @return {@code true}: 是<br>{@code false}: 否
     */
    private boolean isAppDebug() {
        if (TextUtils.isEmpty(this.getPackageName())) return false;
        try {
            PackageManager pm = this.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(this.getPackageName(), 0);
            isDebug = ai != null && (ai.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
            return isDebug;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * 数据库调试，在浏览器中输入地址后可以方便的操作数据库
     *
     * @param context
     */
    public void showDebugDBAddressLogToast(Context context) {
        if (isDebug) {
            try {
                Class<?> debugDB = Class.forName("com.amitshekhar.DebugDB");
                Method getAddressLog = debugDB.getMethod("getAddressLog");
                Object value = getAddressLog.invoke(null);

                Toast.makeText(context, "DB Debug Address: " + (String) value, Toast.LENGTH_LONG).show();
            } catch (Exception ignore) {

            }
        }
    }


    /**
     * 是否是Debug
     *
     * @return
     */
    public boolean isDebug() {
        return isDebug;
    }


    /**
     * 获取进程名字
     *
     * @return
     */
    public String getMyProcessName() {
        try {
            File file = new File("/proc/" + android.os.Process.myPid() + "/" + "cmdline");
            BufferedReader mBufferedReader = new BufferedReader(new FileReader(file));
            String processName = mBufferedReader.readLine().trim();
            mBufferedReader.close();
            return processName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


}
