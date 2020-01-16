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

import android.app.Application;
import android.content.Context;

import me.weishu.leoric.Leoric;
import me.weishu.leoric.LeoricConfigs;

public class MainApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Leoric.init(base, new LeoricConfigs(
                new LeoricConfigs.LeoricConfig(
                        getPackageName() + ":resident",
                        Service1.class.getCanonicalName(),
                        Receiver1.class.getCanonicalName(),
                        Activity1.class.getCanonicalName()),
                new LeoricConfigs.LeoricConfig(
                        "android.media",
                        Service2.class.getCanonicalName(),
                        Receiver2.class.getCanonicalName(),
                        Activity2.class.getCanonicalName())
        ));
    }
}
