/*
IncallActivity.java
Copyright (C) 2011  Belledonne Communications, Grenoble, France

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/
package org.linphone;

import jp.co.aircast.module.R;

import org.linphone.core.Log;
import org.linphone.core.LinphoneCore.RegistrationState;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class FirstLoginActivity extends Activity implements OnClickListener {

	private TextView login;
	private TextView password;
	private SharedPreferences mPref;
	private ProgressBar bar;
	public static FirstLoginActivity instance;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.get("layout", "first_login_view"));
		mPref = PreferenceManager.getDefaultSharedPreferences(this);
		setDefaultDomain(getString(R.get("string", "default_domain")));
		
		login = (TextView) findViewById(R.get("id", "login"));
		login.setText(mPref.getString(getString(R.get("string", "pref_username_key")), ""));

		password = (TextView) findViewById(R.get("id", "password"));
        password.setText(mPref.getString(getString(R.get("string", "pref_passwd_key")), ""));

        bar = (ProgressBar) findViewById(R.get("id", "progress_bar"));
        bar.setVisibility(View.INVISIBLE);

        
		findViewById(R.get("id", "connect")).setOnClickListener(this);
		instance = this;
	}

	private void setDefaultDomain(String string) {
		String domain = mPref.getString(getString(R.get("string", "pref_domain_key")), "");
		if (domain.length() != 0) return;

		writePreference(R.get("string", "pref_domain_key"), getString(R.get("string", "default_domain")));
	}

	public void onClick(View v) {
		if (login.getText() == null || login.length() == 0
				|| password.getText() == null || password.length() == 0) {
			toast(R.get("string", "first_launch_no_login_password"));
			return;
		}

        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);

		writePreference(R.get("string", "pref_username_key"), login.getText().toString());
		writePreference(R.get("string", "pref_passwd_key"), password.getText().toString());

		LinphoneManager.getInstance().initializePayloads();

		try {
			LinphoneManager.getInstance().initFromConf();
		} catch (Throwable e) {
			Log.e(e, "Error while initializing from config in first login activity");
            toast(R.get("string", "error"));;
		}
	}

	
	
	private void writePreference(int key, String value) {
		mPref.edit().putString(getString(key), value).commit();
	}

	@Override
	protected void onDestroy() {
		instance = null;
		super.onDestroy();
	}

	public void onRegistrationStateChanged(RegistrationState state) {
		if (RegistrationState.RegistrationOk == state) {
			bar.setVisibility(View.INVISIBLE);
			toast(R.get("string", "first_launch_ok"));
			mPref.edit().putBoolean(getString(R.get("string", "first_launch_suceeded_once_key")), true).commit();
			setResult(RESULT_OK);
			finish();
		} else if (RegistrationState.RegistrationFailed == state) {
			bar.setVisibility(View.INVISIBLE);
			toast(R.get("string", "first_launch_bad_login_password"));
		} else if (RegistrationState.RegistrationProgress == state) {
			bar.setVisibility(View.VISIBLE);
		}
	}

	private void toast(int key) {
		Toast.makeText(instance, instance.getString(key), Toast.LENGTH_LONG).show();
	}

}
