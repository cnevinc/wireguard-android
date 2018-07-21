/*
 * Copyright © 2018 Jason A. Donenfeld <Jason@zx2c4.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDelegate;

import com.wireguard.android.backend.Backend;
import com.wireguard.android.backend.GoBackend;
import com.wireguard.android.backend.WgQuickBackend;
import com.wireguard.android.configStore.ConfigStore;
import com.wireguard.android.configStore.FileConfigStore;
import com.wireguard.android.model.TunnelManager;
import com.wireguard.android.util.AsyncWorker;
import com.wireguard.android.util.RootShell;
import com.wireguard.android.util.ToolsInstaller;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.Executor;

public class Application extends android.app.Application {
    @SuppressWarnings("NullableProblems") private static WeakReference<Application> weakSelf;
    private final Object haveBackendCallbacksLock = new Object();
    @SuppressWarnings("NullableProblems") private AsyncWorker asyncWorker;
    @Nullable private Backend backend;
    @SuppressWarnings("NullableProblems") private Handler handler;
    @Nullable private Collection<BackendCallback> haveBackendCallbacks = new ArrayList<>();
    @SuppressWarnings("NullableProblems") private RootShell rootShell;
    @SuppressWarnings("NullableProblems") private SharedPreferences sharedPreferences;
    @SuppressWarnings("NullableProblems") private ToolsInstaller toolsInstaller;
    @SuppressWarnings("NullableProblems") private TunnelManager tunnelManager;

    public Application() {
        weakSelf = new WeakReference<>(this);
    }

    public static Application get() {
        return weakSelf.get();
    }

    public static AsyncWorker getAsyncWorker() {
        return get().asyncWorker;
    }

    public static Backend getBackend() {
        final Application app = get();
        synchronized (app) {
            if (app.backend == null) {
                if (new File("/sys/module/wireguard").exists()) {
                    try {
                        app.rootShell.start();
                        app.backend = new WgQuickBackend(app.getApplicationContext());
                    } catch (final Exception ignored) {
                    }
                }
                if (app.backend == null)
                    app.backend = new GoBackend(app.getApplicationContext());
                synchronized (app.haveBackendCallbacksLock) {
                    if (app.haveBackendCallbacks != null) {
                        for (final BackendCallback callback : app.haveBackendCallbacks)
                            app.handler.post(() -> callback.callback(app.backend));
                        app.haveBackendCallbacks = null;
                    }
                }
            }
            return app.backend;
        }
    }

    public static RootShell getRootShell() {
        return get().rootShell;
    }

    public static SharedPreferences getSharedPreferences() {
        return get().sharedPreferences;
    }

    public static ToolsInstaller getToolsInstaller() {
        return get().toolsInstaller;
    }

    public static TunnelManager getTunnelManager() {
        return get().tunnelManager;
    }

    public static void onHaveBackend(final BackendCallback callback) {
        final Application app = get();
        synchronized (app.haveBackendCallbacksLock) {
            if (app.haveBackendCallbacks == null) {
                Objects.requireNonNull(app.backend, "Backend still null in onHaveBackend call");
                callback.callback(app.backend);
            } else {
                app.haveBackendCallbacks.add(callback);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        handler = new Handler(Looper.getMainLooper());
        final Executor executor = AsyncTask.SERIAL_EXECUTOR;
        final ConfigStore configStore = new FileConfigStore(getApplicationContext());

        asyncWorker = new AsyncWorker(executor, handler);
        rootShell = new RootShell(getApplicationContext());
        toolsInstaller = new ToolsInstaller(getApplicationContext());

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        AppCompatDelegate.setDefaultNightMode(
                sharedPreferences.getBoolean("dark_theme", false) ?
                        AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        tunnelManager = new TunnelManager(configStore);
        asyncWorker.runAsync(Application::getBackend);
        tunnelManager.onCreate();
    }

    @FunctionalInterface
    public interface BackendCallback {
        void callback(final Backend backend);
    }
}
