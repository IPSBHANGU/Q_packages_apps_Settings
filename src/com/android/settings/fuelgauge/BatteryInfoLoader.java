/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.fuelgauge;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.BatteryStats;
import android.os.SystemClock;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.utils.AsyncLoader;

/**
 * Loader that can be used by classes to load BatteryInfo in a background thread. This loader will
 * automatically grab enhanced battery estimates if available or fall back to the system estimate
 * when not available.
 */
public class BatteryInfoLoader extends AsyncLoader<BatteryInfo>{
    BatteryStatsHelper mStatsHelper;

    public BatteryInfoLoader(Context context, BatteryStatsHelper batteryStatsHelper) {
        super(context);
        mStatsHelper = batteryStatsHelper;
    }

    @Override
    protected void onDiscardResult(BatteryInfo result) {

    }

    @Override
    public BatteryInfo loadInBackground() {
        Context context = getContext();
        PowerUsageFeatureProvider powerUsageFeatureProvider =
                FeatureFactory.getFactory(context).getPowerUsageFeatureProvider(context);

        // Stuff we always need to get BatteryInfo
        BatteryUtils batteryUtils = BatteryUtils.getInstance(context);
        Intent batteryBroadcast = getContext().registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        final long elapsedRealtimeUs = batteryUtils.convertMsToUs(SystemClock.elapsedRealtime());
        BatteryInfo batteryInfo;

        // Get enhanced prediction if available, otherwise use the old prediction code
        Cursor cursor = null;
        if (powerUsageFeatureProvider.isEnhancedBatteryPredictionEnabled(context)) {
            final Uri queryUri = powerUsageFeatureProvider.getEnhancedBatteryPredictionUri();
            cursor = context.getContentResolver().query(queryUri, null, null, null, null);
        }
        if (cursor != null && cursor.moveToFirst()) {
            long enhancedEstimate = powerUsageFeatureProvider.getTimeRemainingEstimate(cursor);
            batteryInfo = BatteryInfo.getBatteryInfo(context, batteryBroadcast,
                    mStatsHelper.getStats(), elapsedRealtimeUs, false /* shortString */,
                    batteryUtils.convertMsToUs(enhancedEstimate), true /* basedOnUsage */);
        } else {
            BatteryStats stats = mStatsHelper.getStats();
            batteryInfo = BatteryInfo.getBatteryInfo(context, batteryBroadcast, stats,
                    elapsedRealtimeUs, false /* shortString */,
                    stats.computeBatteryTimeRemaining(elapsedRealtimeUs), false /* basedOnUsage */);
        }

        return batteryInfo;
    }
}