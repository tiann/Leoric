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

package me.weishu.leoric.demo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;


public class Service1 extends Service {
    private final static int NOTIFY_ID = 101;
    Notification notification;
    NotificationManager manager;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        showNotification();
        startForeground(NOTIFY_ID, notification);
        return super.onStartCommand(intent, flags, startId);
    }

    private void showNotification() {
        manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Intent hangIntent = new Intent(this, MainActivity.class);

        PendingIntent hangPendingIntent = PendingIntent.getActivity(this, 1002, hangIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String CHANNEL_ID = "your_custom_id";//应用频道Id唯一值， 长度若太长可能会被截断，
        String CHANNEL_NAME = "your_custom_name";//最长40个字符，太长会被截断
        notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("通知Title")
                .setContentText("通知Content")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(hangPendingIntent)
                .setAutoCancel(true)
                .build();

        //Android 8.0 以上需包添加渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID,
                    CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
            manager.createNotificationChannel(notificationChannel);
        }

        manager.notify(NOTIFY_ID, notification);
    }
}
