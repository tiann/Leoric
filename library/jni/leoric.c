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

#include <jni.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <sys/file.h>
#include <sys/inotify.h>
#include <sys/wait.h>
#include <malloc.h>
#include <unistd.h>
#include <stdlib.h>
#include <sys/prctl.h>

#include <android/log.h>

#define TAG		"Leoric"
#define LOGI(...)	__android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGD(...)	__android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGW(...)	__android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define	LOGE(...)	__android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

#define	DAEMON_CALLBACK_NAME		"onDaemonDead"

void waitfor_self_observer(char *observer_file_path) {
    int lockFileDescriptor = open(observer_file_path, O_RDONLY);
    if (lockFileDescriptor == -1) {
        LOGE("Watched >>>>OBSERVER<<<< has been ready before watching...");
        return;
    }

    void *p_buf = malloc(sizeof(struct inotify_event));
    if (p_buf == NULL) {
        LOGE("malloc failed !!!");
        return;
    }
    int maskStrLength = 7 + 10 + 1;
    char *p_maskStr = malloc(maskStrLength);
    if (p_maskStr == NULL) {
        free(p_buf);
        LOGE("malloc failed !!!");
        return;
    }
    int fileDescriptor = inotify_init();
    if (fileDescriptor < 0) {
        free(p_buf);
        free(p_maskStr);
        LOGE("inotify_init failed !!!");
        return;
    }

    int watchDescriptor = inotify_add_watch(fileDescriptor, observer_file_path, IN_ALL_EVENTS);
    if (watchDescriptor < 0) {
        free(p_buf);
        free(p_maskStr);
        LOGE("inotify_add_watch failed !!!");
        return;
    }


    while (1) {
        size_t readBytes = read(fileDescriptor, p_buf, sizeof(struct inotify_event));
        if (4 == ((struct inotify_event *) p_buf)->mask) {
            LOGE("Watched >>>>OBSERVER<<<< has been ready...");
            free(p_maskStr);
            free(p_buf);
            return;
        }
    }
}

void notify_daemon_observer(unsigned char is_persistent, char *observer_file_path) {
    if (!is_persistent) {
        int lockFileDescriptor = open(observer_file_path, O_RDONLY);
        while (lockFileDescriptor == -1) {
            lockFileDescriptor = open(observer_file_path, O_RDONLY);
        }
    }
    remove(observer_file_path);
}

void notify_and_waitfor(char *observer_self_path, char *observer_daemon_path) {
    int observer_self_descriptor = open(observer_self_path, O_RDONLY);
    if (observer_self_descriptor == -1) {
        observer_self_descriptor = open(observer_self_path, O_CREAT, S_IRUSR | S_IWUSR);
    }
    int observer_daemon_descriptor = open(observer_daemon_path, O_RDONLY);
    while (observer_daemon_descriptor == -1) {
        usleep(1000);
        observer_daemon_descriptor = open(observer_daemon_path, O_RDONLY);
    }
    remove(observer_daemon_path);
    LOGE("Watched >>>>OBSERVER<<<< has been ready...");
}


int lock_file(char *lock_file_path) {
    LOGD("start try to lock file >> %s <<", lock_file_path);
    int lockFileDescriptor = open(lock_file_path, O_RDONLY);
    if (lockFileDescriptor == -1) {
        lockFileDescriptor = open(lock_file_path, O_CREAT, S_IRUSR);
    }
    int lockRet = flock(lockFileDescriptor, LOCK_EX);
    if (lockRet == -1) {
        LOGE("lock file failed >> %s <<", lock_file_path);
        return 0;
    } else {
        LOGD("lock file success  >> %s <<", lock_file_path);
        return 1;
    }
}

void java_callback(JNIEnv *env, jobject jobj, char *method_name) {
    jclass cls = (*env)->GetObjectClass(env, jobj);
    jmethodID cb_method = (*env)->GetMethodID(env, cls, method_name, "()V");
    (*env)->CallVoidMethod(env, jobj, cb_method);
}

void do_daemon(JNIEnv *env, jobject jobj, char *indicator_self_path, char *indicator_daemon_path,
               char *observer_self_path, char *observer_daemon_path) {
    int lock_status = 0;
    int try_time = 0;
    while (try_time < 3 && !(lock_status = lock_file(indicator_self_path))) {
        try_time++;
        LOGD("Persistent lock myself failed and try again as %d times", try_time);
        usleep(10000);
    }
    if (!lock_status) {
        LOGE("Persistent lock myself failed and exit");
        return;
    }

    notify_and_waitfor(observer_self_path, observer_daemon_path);

    lock_status = lock_file(indicator_daemon_path);
    if (lock_status) {
        LOGE("Watch >>>>DAEMON<<<<< Daed !!");
        remove(observer_self_path);// it`s important ! to prevent from deadlock
        java_callback(env, jobj, DAEMON_CALLBACK_NAME);
    }
}

void create_file_if_not_exist(char *path) {
    FILE *fp = fopen(path, "ab+");
    if (fp) {
        fclose(fp);
    }
}

void set_process_name(JNIEnv *env) {
    jclass process = (*env)->FindClass(env, "android/os/Process");
    jmethodID setArgV0 = (*env)->GetStaticMethodID(env, process, "setArgV0",
                                                   "(Ljava/lang/String;)V");
    jstring name = (*env)->NewStringUTF(env, "app_d");
    (*env)->CallStaticVoidMethod(env, process, setArgV0, name);
}

JNIEXPORT void JNICALL
Java_me_weishu_leoric_NativeLeoric_doDaemon(JNIEnv *env, jobject jobj,
                                                               jstring indicatorSelfPath,
                                                               jstring indicatorDaemonPath,
                                                               jstring observerSelfPath,
                                                               jstring observerDaemonPath) {
    if (indicatorSelfPath == NULL || indicatorDaemonPath == NULL || observerSelfPath == NULL ||
        observerDaemonPath == NULL) {
        LOGE("parameters cannot be NULL !");
        return;
    }

    char *indicator_self_path = (char *) (*env)->GetStringUTFChars(env, indicatorSelfPath, 0);
    char *indicator_daemon_path = (char *) (*env)->GetStringUTFChars(env, indicatorDaemonPath, 0);
    char *observer_self_path = (char *) (*env)->GetStringUTFChars(env, observerSelfPath, 0);
    char *observer_daemon_path = (char *) (*env)->GetStringUTFChars(env, observerDaemonPath, 0);


    pid_t pid;
    if ((pid = fork()) < 0) {
        printf("fork 1 error\n");
        exit(-1);
    } else if (pid == 0) { //第一个子进程
        if ((pid = fork()) < 0) {
            printf("fork 2 error\n");
            exit(-1);
        } else if (pid > 0) {
            // 托孤
            exit(0);
        }

        LOGD("mypid: %d", getpid());
        const int MAX_PATH = 256;
        char indicator_self_path_child[MAX_PATH];
        char indicator_daemon_path_child[MAX_PATH];
        char observer_self_path_child[MAX_PATH];
        char observer_daemon_path_child[MAX_PATH];

        strcpy(indicator_self_path_child, indicator_self_path);
        strcat(indicator_self_path_child, "-c");

        strcpy(indicator_daemon_path_child, indicator_daemon_path);
        strcat(indicator_daemon_path_child, "-c");

        strcpy(observer_self_path_child, observer_self_path);
        strcat(observer_self_path_child, "-c");

        strcpy(observer_daemon_path_child, observer_daemon_path);
        strcat(observer_daemon_path_child, "-c");

        create_file_if_not_exist(indicator_self_path_child);
        create_file_if_not_exist(indicator_daemon_path_child);

        set_process_name(env);

        do_daemon(env, jobj, indicator_self_path_child, indicator_daemon_path_child,
                  observer_self_path_child, observer_daemon_path_child);
        return;
    }

    if (waitpid(pid, NULL, 0) != pid)
        printf("waitpid error\n");


    do_daemon(env, jobj, indicator_self_path, indicator_daemon_path, observer_self_path,
              observer_daemon_path);
}
