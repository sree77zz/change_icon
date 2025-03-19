package com.example.changeicon;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import io.flutter.Log;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.embedding.engine.plugins.lifecycle.FlutterLifecycleAdapter;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodChannel;

/** ChangeiconPlugin */
public class ChangeiconPlugin implements FlutterPlugin, ActivityAware {
    private MethodChannel channel;
    private MethodCallImplementation handler;
    private static final String TAG = "[Change_icon]";
    private static final String CHANNEL_ID = "Changeicon";
    private FlutterPluginBinding pluginBinding;
    private ActivityState activityState;

    public static String getTAG() {
        return TAG;
    }

    public static String getChannelId() {
        return CHANNEL_ID;
    }

    private void setupChannel(BinaryMessenger messenger, Context context, Activity activity) {
        channel = new MethodChannel(messenger, CHANNEL_ID);
        handler = new MethodCallImplementation(context, activity);
        channel.setMethodCallHandler(handler);
    }

    private void teardownChannel() {
        if (channel != null) {
            channel.setMethodCallHandler(null);
            channel = null;
        }
        handler = null;
    }

    private class LifeCycleObserver implements Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {
        private final Activity thisActivity;

        LifeCycleObserver(Activity activity) {
            this.thisActivity = activity;
        }

        @Override
        public void onPause(@NonNull LifecycleOwner owner) {
            Log.i("ChangeIcon", "The app has paused");
            handler.updateIcon();
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            if (thisActivity == activity && activity.getApplicationContext() != null) {
                ((Application) activity.getApplicationContext())
                        .unregisterActivityLifecycleCallbacks(this);
            }
        }

        @Override public void onCreate(@NonNull LifecycleOwner owner) {}
        @Override public void onStart(@NonNull LifecycleOwner owner) {}
        @Override public void onResume(@NonNull LifecycleOwner owner) {}
        @Override public void onStop(@NonNull LifecycleOwner owner) {}
        @Override public void onDestroy(@NonNull LifecycleOwner owner) {}
        @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}
        @Override public void onActivityStarted(Activity activity) {}
        @Override public void onActivityResumed(Activity activity) {}
        @Override public void onActivityPaused(Activity activity) {}
        @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
        @Override public void onActivityStopped(Activity activity) {}
    }

    private class ActivityState {
        private Application application;
        private Activity activity;
        private LifeCycleObserver observer;
        private ActivityPluginBinding activityBinding;
        private BinaryMessenger messenger;
        private Lifecycle lifecycle;

        ActivityState(Application application, Activity activity, BinaryMessenger messenger, ActivityPluginBinding activityBinding) {
            this.application = application;
            this.activity = activity;
            this.activityBinding = activityBinding;
            this.messenger = messenger;
            observer = new LifeCycleObserver(activity);
            if (activityBinding != null) {
                lifecycle = FlutterLifecycleAdapter.getActivityLifecycle(activityBinding);
                lifecycle.addObserver(observer);
            }
        }

        void release() {
            if (lifecycle != null) {
                lifecycle.removeObserver(observer);
                lifecycle = null;
            }

            if (application != null) {
                application.unregisterActivityLifecycleCallbacks(observer);
                application = null;
            }

            activityBinding = null;
            activity = null;
            observer = null;
        }

        Activity getActivity() {
            return activity;
        }
    }

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        pluginBinding = flutterPluginBinding;
        setupChannel(flutterPluginBinding.getBinaryMessenger(), flutterPluginBinding.getApplicationContext(), null);
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        setup(
                pluginBinding.getBinaryMessenger(),
                (Application) pluginBinding.getApplicationContext(),
                binding.getActivity(),
                binding
        );
        handler.setActivity(binding.getActivity());
    }

    @VisibleForTesting
    final ActivityState getActivityState() {
        return activityState;
    }

    private void setup(BinaryMessenger messenger, Application application, Activity activity, ActivityPluginBinding activityBinding) {
        activityState = new ActivityState(application, activity, messenger, activityBinding);
    }

    private void tearDown() {
        if (activityState != null) {
            activityState.release();
            activityState = null;
        }
    }

    @Override
    public void onDetachedFromActivity() {
        tearDown();
        handler.setActivity(null);
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity();
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        onAttachedToActivity(binding);
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        teardownChannel();
    }
}
