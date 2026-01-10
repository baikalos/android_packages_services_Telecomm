/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.server.telecom.callfiltering;

import android.app.ActivityManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog;
import android.telecom.Log;

import com.android.server.telecom.Call;
import com.android.server.telecom.CallerInfoLookupHelper;
import com.android.server.telecom.LogUtils;
import com.android.server.telecom.util.CallerInfo;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class BaikalCallFilter extends CallFilter {
    private final Context mContext;
    private final Call mCall;
    private final CallerInfoLookupHelper mCallerInfoLookupHelper;

    public BaikalCallFilter(Context context, Call call, CallerInfoLookupHelper callerInfoLookupHelper) {
        mContext = context;
        mCall = call;
        mCallerInfoLookupHelper = callerInfoLookupHelper;
    }

    @Override
    public CompletionStage<CallFilteringResult> startFilterLookup(CallFilteringResult result) {
        Log.addEvent(mCall, LogUtils.Events.DIRECT_TO_VM_INITIATED);
        CompletableFuture<CallFilteringResult> resultFuture = new CompletableFuture<>();
        mCallerInfoLookupHelper.startLookup(mCall.getHandle(),
                new CallerInfoLookupHelper.OnQueryCompleteListener() {
                    @Override
                    public void onCallerInfoQueryComplete(Uri handle, CallerInfo info) {

                        Bundle params = new Bundle();
                        params.putString("callerDisplayName", mCall.getPhoneNumber() /*getCallerDisplayName()*/);
                        params.putString("contactDisplayName", mCall.getName() /*info != null ? info.getName() : "<unknown>"*/);
 
                        boolean shouldReject = mContext.getBaikalContext().getBaikalOptionWithParams(0,0,-1,null,params) != 0;
                            resultFuture.complete(new CallFilteringResult.Builder()
                                    .setShouldAllowCall(true)
                                    .setShouldAddToCallLog(true)
                                    .setShouldShowNotification(true)
                                    .setDndSuppressed(shouldReject)
                                    .build());

                        Log.addEvent(mCall, LogUtils.Events.DIRECT_TO_VM_FINISHED);
                    }

                    @Override
                    public void onContactPhotoQueryComplete(Uri handle, CallerInfo info) {
                        // Ignore
                    }
                });
        return resultFuture;
    }
}
