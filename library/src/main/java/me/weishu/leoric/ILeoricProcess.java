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

import android.content.Context;
import android.os.Build;

public interface ILeoricProcess {
	/**
	 * Initialization some files or other when 1st time 
	 */
	boolean onInit(Context context);

	/**
	 * when Persistent processName create
	 * 
	 */
	void onPersistentCreate(Context context, LeoricConfigs configs);

	/**
	 * when DaemonAssistant processName create
	 */
	void onDaemonAssistantCreate(Context context, LeoricConfigs configs);

	/**
	 * when watches the processName dead which it watched
	 */
	void onDaemonDead();

	
	class Fetcher {

		private static volatile ILeoricProcess mDaemonStrategy;

		/**
		 * fetch the strategy for this device
		 * 
		 * @return the daemon strategy for this device
		 */
		static ILeoricProcess fetchStrategy() {
			if (mDaemonStrategy != null) {
				return mDaemonStrategy;
			}
			int sdk = Build.VERSION.SDK_INT;
			mDaemonStrategy = new LeoricProcessImpl();
			return mDaemonStrategy;
		}
	}
}
