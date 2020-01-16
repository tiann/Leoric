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

public class LeoricConfigs {

    public final LeoricConfig PERSISTENT_CONFIG;
    public final LeoricConfig DAEMON_ASSISTANT_CONFIG;

    public LeoricConfigs(LeoricConfig persistentConfig, LeoricConfig daemonAssistantConfig) {
        this.PERSISTENT_CONFIG = persistentConfig;
        this.DAEMON_ASSISTANT_CONFIG = daemonAssistantConfig;
    }

    public static class LeoricConfig {

        final String processName;
        final String serviceName;
        final String receiverName;
        final String activityName;

        public LeoricConfig(String processName, String serviceName, String receiverName, String activityName) {
            this.processName = processName;
            this.serviceName = serviceName;
            this.receiverName = receiverName;
            this.activityName = activityName;
        }
    }
}
