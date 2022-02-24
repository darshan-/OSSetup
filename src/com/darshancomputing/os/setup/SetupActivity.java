package com.darshancomputing.os.setup;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.UiModeManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.display.ColorDisplayManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Display;

import com.android.settingslib.display.DisplayDensityConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;

public class SetupActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //new Exception("Stack trace").printStackTrace();
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        PackageManager pm = getPackageManager();
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");

        intent.setPackage(getPackageName());
        ComponentName cn = intent.resolveActivity(pm);
        if (cn == null) {
            System.out.println("darshanos: Looks like we're already not the home activity, so quitting early.");
            finish();  // Says duplicate... even though activity has been recreated?  But calling it also gets complaint.
            return;
        }

        ContentResolver cr = getContentResolver();

        getSystemService(UiModeManager.class).setNightModeActivated(true);
        ((AlarmManager) getSystemService(Context.ALARM_SERVICE)).setTimeZone("America/Los_Angeles");
        DisplayDensityConfiguration.setForcedDisplayDensity(Display.DEFAULT_DISPLAY, 460);
        Settings.Secure.putInt(cr, Settings.Secure.DOZE_ALWAYS_ON, 1);
        //Settings.Secure.putInt(cr, Settings.Secure.WAKE_GESTURE_ENABLED, 0);
        getSystemService(ColorDisplayManager.class).setColorMode(ColorDisplayManager.COLOR_MODE_NATURAL);
        Settings.Secure.putInt(cr, Settings.Secure.DOZE_PICK_UP_GESTURE, 0);
        Settings.Secure.putInt(cr, Settings.Secure.DOZE_ENABLED, 0);
        Settings.Secure.putInt(cr, Settings.Secure.CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED, 1);


        Settings.Global.putInt(cr, Settings.Global.DEVICE_PROVISIONED, 1);
        Settings.Secure.putInt(cr, Settings.Secure.USER_SETUP_COMPLETE, 1);

        System.out.println("darshanos: SetupActivity.onResume done, just need to disable self as home activity and call finish()");

        // Make us not the home app activity anymore.
        pm.setComponentEnabledSetting(cn,
                                      PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                      PackageManager.DONT_KILL_APP);

        // startActivity(new Intent("android.intent.action.MAIN")
        //          .addCategory("android.intent.category.HOME")
        //          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));

        finish();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
