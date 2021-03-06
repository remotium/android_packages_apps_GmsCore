/*
 * Copyright 2013-2015 µg Project Team
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

package org.microg.gms.checkin;

import android.util.Log;

import com.squareup.wire.Wire;

import org.microg.gms.common.Build;
import org.microg.gms.common.Constants;
import org.microg.gms.common.DeviceConfiguration;
import org.microg.gms.common.DeviceIdentifier;
import org.microg.gms.common.PhoneInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CheckinClient {
    private static final String TAG = "GmsCheckinClient";
    private static final Object TODO = null; // TODO
    private static final String SERVICE_URL = "https://android.clients.google.com/checkin";

    public static CheckinResponse request(CheckinRequest request) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(SERVICE_URL).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-protobuffer");
        connection.setRequestProperty("User-Agent", "Android-Checkin/2.0");

        Log.d(TAG, "-- Request --\n" + request);
        OutputStream os = connection.getOutputStream();
        os.write(request.toByteArray());
        os.close();

        if (connection.getResponseCode() != 200) {
            throw new IOException(connection.getResponseMessage());
        }

        InputStream is = connection.getInputStream();
        CheckinResponse response = new Wire().parseFrom(is, CheckinResponse.class);
        is.close();
        return response;
    }

    private static CheckinRequest.Checkin.Build makeBuild(Build build) {
        return new CheckinRequest.Checkin.Build.Builder()
                .bootloader(build.bootloader)
                .brand(build.brand)
                .clientId((String) TODO)
                .device(build.device)
                .fingerprint(build.fingerprint)
                .hardware(build.hardware)
                .manufacturer(build.manufacturer)
                .model(build.model)
                .otaInstalled(false) // TODO?
                .packageVersionCode(Constants.MAX_REFERENCE_VERSION)
                .product(build.product)
                .radio(build.radio)
                .sdkVersion(build.sdk)
                .time(build.time / 1000)
                .build();
    }

    private static CheckinRequest.DeviceConfig makeDeviceConfig(DeviceConfiguration deviceConfiguration) {
        return new CheckinRequest.DeviceConfig.Builder()
                .availableFeature(deviceConfiguration.availableFeatures)
                .densityDpi(deviceConfiguration.densityDpi)
                .glEsVersion(deviceConfiguration.glEsVersion)
                .glExtension(deviceConfiguration.glExtensions)
                .hasFiveWayNavigation(deviceConfiguration.hasFiveWayNavigation)
                .hasHardKeyboard(deviceConfiguration.hasHardKeyboard)
                .heightPixels(deviceConfiguration.heightPixels)
                .keyboardType(deviceConfiguration.keyboardType)
                .locale(deviceConfiguration.locales)
                .nativePlatform(deviceConfiguration.nativePlatforms)
                .navigation(deviceConfiguration.navigation)
                .screenLayout(deviceConfiguration.screenLayout)
                .sharedLibrary(deviceConfiguration.sharedLibraries)
                .touchScreen(deviceConfiguration.touchScreen)
                .widthPixels(deviceConfiguration.widthPixels)
                .build();
    }

    private static CheckinRequest.Checkin makeCheckin(CheckinRequest.Checkin.Build build,
                                                      PhoneInfo phoneInfo, LastCheckinInfo checkinInfo) {
        return new CheckinRequest.Checkin.Builder()
                .build(build)
                .cellOperator(phoneInfo.cellOperator)
                .event((List<CheckinRequest.Checkin.Event>) TODO)
                .lastCheckinMs(checkinInfo.lastCheckin)
                .requestedGroup((List<String>) TODO)
                .roaming(phoneInfo.roaming)
                .simOperator(phoneInfo.simOperator)
                .stat((List<CheckinRequest.Checkin.Statistic>) TODO)
                .userNumber((Integer) TODO)
                .build();
    }

    private static CheckinRequest makeRequest(CheckinRequest.Checkin checkin,
                                              CheckinRequest.DeviceConfig deviceConfig,
                                              DeviceIdentifier deviceIdent,
                                              LastCheckinInfo checkinInfo,
                                              List<Account> accounts) {
        CheckinRequest.Builder builder = new CheckinRequest.Builder()
                .accountCookie(Arrays.asList("")) // TODO
                .androidId(checkinInfo.androidId)
                .checkin(checkin)
                .deviceConfiguration(deviceConfig)
                .digest(checkinInfo.digest)
                .esn(deviceIdent.esn)
                .fragment(0)
                .locale((String) TODO)
                .loggingId((Long) TODO)
                .meid(deviceIdent.meid)
                .otaCert((List<String>) TODO)
                .serial((String) TODO)
                .timeZone((String) TODO)
                .userName((String) TODO)
                .userSerialNumber((Integer) TODO)
                .version(3);
        builder.accountCookie(new ArrayList<String>());
        for (Account account : accounts) {
            builder.accountCookie.add("[" + account.name + "]");
            builder.accountCookie.add(account.authToken);
        }
        if (deviceIdent.wifiMac != null) {
            builder.macAddress(Arrays.asList(deviceIdent.wifiMac))
                    .macAddressType(Arrays.asList("wifi"));
        }
        if (checkinInfo.securityToken != 0) {
            builder.securityToken(checkinInfo.securityToken)
                    .fragment(1);
        }
        return builder.build();

    }

    public static CheckinRequest makeRequest(Build build, DeviceConfiguration deviceConfiguration,
                                             DeviceIdentifier deviceIdent, PhoneInfo phoneInfo,
                                             LastCheckinInfo checkinInfo,
                                             List<Account> accounts) {
        return makeRequest(makeCheckin(makeBuild(build), phoneInfo, checkinInfo),
                makeDeviceConfig(deviceConfiguration), deviceIdent, checkinInfo, accounts);
    }

    public static class Account {
        public final String name;
        public final String authToken;

        public Account(String accountName, String authToken) {
            this.name = accountName;
            this.authToken = authToken;
        }
    }
}
