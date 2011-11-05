/*
 * Copyright (C) 2009 Stefano Sanna
 * 
 * gerdavax@gmail.com - http://www.gerdavax.it
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
package it.gerdavax.android.bluetooth.util;

import android.os.Build;
import android.util.Log;

public class PlatformChecker {
	private static final int ANDROID_1_0 = 1;
	private static final int ANDROID_1_1 = 2;
	private static final int ANDROID_1_5 = 3;
	private static final int ANDROID_1_6 = 4;
	private static final String TAG = "BluetoothAPI";
	
	private PlatformChecker() {
		
	}
	
	public static boolean isThisPlatformSupported() {
		printPlatformDescription();
		
		int platform = Integer.parseInt(Build.VERSION.SDK);
		
		boolean supported = false;
		
		switch (platform) {
			case ANDROID_1_0:
				// never tested!
				supported = false;
				break;
			case ANDROID_1_1:
				// hopefully it can be only an HTC Dream
				supported = true;
				break;
			case ANDROID_1_5:
				supported = true;
				break;
			case ANDROID_1_6:
				// not yet tested
				break;
		}
		return supported;
	}
	
	public static void printPlatformDescription() {
		System.out.println("Android Bluetooth API - Platform checker");
		System.out.println("SDK: " + Build.VERSION.SDK);
		System.out.println("Board: " + Build.BOARD);
		System.out.println("Brand: " + Build.BRAND);
		System.out.println("Device: " + Build.DEVICE);
		System.out.println("Display: " + Build.DISPLAY);
		System.out.println("Fingerprint: " + Build.FINGERPRINT);
		System.out.println("Host: " + Build.HOST);
		System.out.println("ID: " + Build.ID);
		System.out.println("Model: " + Build.MODEL);
		System.out.println("Product: " + Build.PRODUCT);
		System.out.println("Tags: " + Build.TAGS);
		System.out.println("Time: " + Build.TIME);
		System.out.println("Type: " + Build.TYPE);
		System.out.println("User: " + Build.USER);
	}
}
