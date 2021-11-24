/*
 * Original Copyright 2015 Mars Kwok
 * Modified work Copyright (c) 2020, weishu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.weishu.leoric;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

public class LeoricProcessImpl implements ILeoricProcess {

    private static final String TAG = "LeoricProcessImpl";

    private final static String INDICATOR_DIR_NAME = "indicators";
    private final static String INDICATOR_PERSISTENT_FILENAME = "indicator_p";
    private final static String INDICATOR_DAEMON_ASSISTANT_FILENAME = "indicator_d";
    private final static String OBSERVER_PERSISTENT_FILENAME = "observer_p";
    private final static String OBSERVER_DAEMON_ASSISTANT_FILENAME = "observer_d";

    private IBinder mRemote;
    private Parcel mServiceData;

    private int mPid = Process.myPid();

    @Override
    public boolean onInit(Context context) {
        return initIndicatorFiles(context);
    }

    @Override
    public void onPersistentCreate(final Context context, LeoricConfigs configs) {

        initAmsBinder();
        initServiceParcel(context, configs.DAEMON_ASSISTANT_CONFIG.serviceName);
        startServiceByAmsBinder();

        Thread t = new Thread() {
            public void run() {
                File indicatorDir = context.getDir(INDICATOR_DIR_NAME, Context.MODE_PRIVATE);
                new NativeLeoric().doDaemon(
                        new File(indicatorDir, INDICATOR_PERSISTENT_FILENAME).getAbsolutePath(),
                        new File(indicatorDir, INDICATOR_DAEMON_ASSISTANT_FILENAME).getAbsolutePath(),
                        new File(indicatorDir, OBSERVER_PERSISTENT_FILENAME).getAbsolutePath(),
                        new File(indicatorDir, OBSERVER_DAEMON_ASSISTANT_FILENAME).getAbsolutePath());
            }

            ;
        };
        t.start();

    }

    @Override
    public void onDaemonAssistantCreate(final Context context, LeoricConfigs configs) {
        initAmsBinder();
        initServiceParcel(context, configs.PERSISTENT_CONFIG.serviceName);
        startServiceByAmsBinder();

        Thread t = new Thread() {
            public void run() {
                File indicatorDir = context.getDir(INDICATOR_DIR_NAME, Context.MODE_PRIVATE);
                new NativeLeoric().doDaemon(
                        new File(indicatorDir, INDICATOR_DAEMON_ASSISTANT_FILENAME).getAbsolutePath(),
                        new File(indicatorDir, INDICATOR_PERSISTENT_FILENAME).getAbsolutePath(),
                        new File(indicatorDir, OBSERVER_DAEMON_ASSISTANT_FILENAME).getAbsolutePath(),
                        new File(indicatorDir, OBSERVER_PERSISTENT_FILENAME).getAbsolutePath());
            }

            ;
        };
        t.start();

    }


    @Override
    public void onDaemonDead() {
        Log.i(TAG, "on daemon dead!");
        if (startServiceByAmsBinder()) {

            int pid = Process.myPid();
            Log.i(TAG, "mPid: " + mPid + " current pid: " + pid);
            android.os.Process.killProcess(mPid);
        }
    }


    private void initAmsBinder() {
        Class<?> activityManagerNative;
        try {
            activityManagerNative = Class.forName("android.app.ActivityManagerNative");
            Object amn = activityManagerNative.getMethod("getDefault").invoke(activityManagerNative);
            Field mRemoteField = amn.getClass().getDeclaredField("mRemote");
            mRemoteField.setAccessible(true);
            mRemote = (IBinder) mRemoteField.get(amn);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }


    @SuppressLint("Recycle")
// when processName dead, we should save time to restart and kill self, don`t take a waste of time to recycle
    private void initServiceParcel(Context context, String serviceName) {
        Intent intent = new Intent();
        ComponentName component = new ComponentName(context.getPackageName(), serviceName);
        intent.setComponent(component);

        Parcel parcel = Parcel.obtain();
        intent.writeToParcel(parcel, 0);

        mServiceData = Parcel.obtain();
        if (Build.VERSION.SDK_INT >= 26) {
            // Android 8.1
            mServiceData.writeInterfaceToken("android.app.IActivityManager");
            mServiceData.writeStrongBinder(null);
            mServiceData.writeInt(1);
            intent.writeToParcel(mServiceData, 0);
            mServiceData.writeString(null);
            mServiceData.writeInt(context.getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.O ? 1 : 0);
            mServiceData.writeString(context.getPackageName());
            mServiceData.writeInt(0);
        } else {
            // http://aospxref.com/android-7.1.2_r36/xref/frameworks/base/core/java/android/app/ActivityManagerNative.java
            mServiceData.writeInterfaceToken("android.app.IActivityManager");
            mServiceData.writeStrongBinder(null);
            intent.writeToParcel(mServiceData, 0);
            mServiceData.writeString(null);
            if (Build.VERSION.SDK_INT > 22) {//适配5.1(22)
                mServiceData.writeString(context.getPackageName());
            }
            mServiceData.writeInt(0);
        }

    }


    private boolean startServiceByAmsBinder() {
        try {
            if (mRemote == null || mServiceData == null) {
                Log.e("Daemon", "REMOTE IS NULL or PARCEL IS NULL !!!");
                return false;
            }
            int code;
            switch (Build.VERSION.SDK_INT) {
                case 26:
                case 27:
                    code = 26;
                    break;
                case 28:
                    code = 30;
                    break;
                case 29:
                    code = 24;
                    break;
                case 30:
                    code = 26;
                    break;
                case 31:
                    code = 27;
                    break;
                default:
                    code = 34;
                    break;
            }
            //mRemote.transact(34, mServiceData, null, 0);//START_SERVICE_TRANSACTION = 34
//            mRemote.transact(26, mServiceData, null, 1);//START_SERVICE_TRANSACTION = 34
            mRemote.transact(code, mServiceData, null, 1);//START_SERVICE_TRANSACTION = 34
            return true;
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }


    private boolean initIndicatorFiles(Context context) {
        File dirFile = context.getDir(INDICATOR_DIR_NAME, Context.MODE_PRIVATE);
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }
        try {
            createNewFile(dirFile, INDICATOR_PERSISTENT_FILENAME);
            createNewFile(dirFile, INDICATOR_DAEMON_ASSISTANT_FILENAME);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void createNewFile(File dirFile, String fileName) throws IOException {
        File file = new File(dirFile, fileName);
        if (!file.exists()) {
            file.createNewFile();
        }
    }
}
