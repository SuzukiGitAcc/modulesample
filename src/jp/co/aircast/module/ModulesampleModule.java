/**
 * This file was auto-generated by the Titanium Module SDK helper for Android
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 *
 */
package jp.co.aircast.module;

import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;

import org.appcelerator.titanium.TiApplication;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiConfig;
import org.linphone.LinphoneManager;
import org.linphone.LinphonePreferencesActivity;
import org.linphone.core.LinphoneCall;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;


@Kroll.module(name="Modulesample", id="jp.co.aircast.module")
public class ModulesampleModule extends KrollModule
{

	// Standard Debugging variables
	private static final String LCAT = "ModulesampleModule";
	private static final boolean DBG = TiConfig.LOGD;

	// You can define constants with @Kroll.constant, for example:
	// @Kroll.constant public static final String EXTERNAL_NAME = value;
	
	public ModulesampleModule()
	{
		super();
	}

	@Kroll.onAppCreate
	public static void onAppCreate(TiApplication app)
	{
		Log.d(LCAT, "inside onAppCreate");
		// put module init code that needs to run when the application is created
	}
	
	@Kroll.method
	public String getPhoneNumber()
	{
		String ret = null;
		Activity a = TiApplication.getInstance().getCurrentActivity();
		
		TelephonyManager tm = (TelephonyManager) a.getSystemService(Context.TELEPHONY_SERVICE);
		if(tm != null)
			ret = tm.getLine1Number();
		
		return ret;
	}
}
