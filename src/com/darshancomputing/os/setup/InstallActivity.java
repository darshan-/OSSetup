package com.darshancomputing.os.setup;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.os.Bundle;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;

public class InstallActivity extends Activity {
    private static final String ACTION_INSTALL = "com.darshancomputing.install";

    private PackageInstaller pmInstaller;

    private LinkedList<Integer> sessionQueue;

    // Match order for these two, and keep in Graphene's recommended install order
    private static final String[] apkBasenames = {
        "com_google_android_gsf",
        "com_google_android_gms"
    };
    private static final int[] apkResIds = {
        R.raw.com_google_android_gsf,
        R.raw.com_google_android_gms
    };

    // Match order for these two
    private static final String[] vapkBasenames = {
        "vending_0",
        "vending_1",
        "vending_2",
        "vending_3",
        "vending_4"
    };
    private static final int[] vapkResIds = {
        R.raw.vending_0,
        R.raw.vending_1,
        R.raw.vending_2,
        R.raw.vending_3,
        R.raw.vending_4
    };

    private final BroadcastReceiver pmInstallerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            System.out.println("darshanos: pmInstallerReceiver got intent: " + intent);

            if (! ACTION_INSTALL.equals(intent.getAction()))
                return;

            //int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -99); // 0, -1 and small positive already used
            // Ahem... What if... What if we're meant to assume success?  And that's why it's 0, which of course I first
            //    typed, before it occurred to me a moment later that that was unsafe, and I should check if 0 was used,
            //    and it was... for success...  Well, let's see how this goes:
            int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, 0);
            System.out.println("darshanos: install status: " + status);

            String message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
            System.out.println("darshanos: install message: " + message);

            if (status == PackageInstaller.STATUS_SUCCESS) {
                if (sessionQueue.size() > 0) {
                    System.out.println("darshanos: commiting next session because sessionQueue.size(): " + sessionQueue.size());
                    commitSession(sessionQueue.remove());
                } else {
                    System.out.println("darshanos: all sessions commited; finishing.");
                    finish();
                }
            } else {
                System.out.println("darshanos: some status other than success; finishing.");
                finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.install);
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(pmInstallerReceiver, new IntentFilter(ACTION_INSTALL));

        sessionQueue = new LinkedList<Integer>();
        pmInstaller = getPackageManager().getPackageInstaller();
        try {
            for (int i = 0; i < apkResIds.length; i++)
                sessionQueue.add(makeSession(getApkStream(i), apkBasenames[i] + ".apk"));

            installVending();
        } catch (IOException ioe) {
            System.out.println("darshanos: IOException setting up installation sessions: " + ioe);
        }

        System.out.println("darshanos: committing first session with full sessionQueue.size(): " + sessionQueue.size());
        commitSession(sessionQueue.remove());
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(pmInstallerReceiver);
        super.onDestroy();
    }

    private void commitSession(int sid) {
        try {
            PackageInstaller.Session session = pmInstaller.openSession(sid);
            System.out.println("darshanos: committing session with names: " + java.util.Arrays.toString(session.getNames()));
            PendingIntent pi = PendingIntent.getBroadcast(this, sid, new Intent(ACTION_INSTALL), PendingIntent.FLAG_IMMUTABLE);
            session.commit(pi.getIntentSender());
        } catch (IOException ioe) {
            System.out.println("darshanos: IOException committing installation sessions: " + ioe);
        }
    }

    // Caller needs both Session id and Session, but we can't get an id from a Session (Session doesn't know its
    //   own id), but we *can* get a session from an id, so it's dumb, but I guess best, to return the id.
    private int makeInstallSession() throws IOException {
        PackageInstaller.SessionParams params = makeIncompleteInstallSession();
        return pmInstaller.createSession(params);
    }

    private PackageInstaller.SessionParams makeIncompleteInstallSession() throws IOException {
        int mode = PackageInstaller.SessionParams.MODE_FULL_INSTALL;
        return new PackageInstaller.SessionParams(mode);
    }

    private void addApkToSession(PackageInstaller.Session session, InputStream in, String apkName) throws IOException {
        System.out.println("darshanos: Adding apk with name: " + apkName);
        OutputStream out = session.openWrite(apkName, 0, -1);
        byte[] buffer = new byte[65536];
        int c;

        while ((c = in.read(buffer)) != -1)
            out.write(buffer, 0, c);

        session.fsync(out);
        in.close();
        out.close();
    }

    private int makeSession(InputStream in, String apkName) throws IOException {
        int sid = makeInstallSession();
        PackageInstaller.Session session = pmInstaller.openSession(sid);

        addApkToSession(session, in, apkName);
        return sid;
    }

    private void installVending() throws IOException {
        int psid = makeSession(getVapkStream(0), vapkBasenames[0] + ".apk");
        PackageInstaller.Session psession = pmInstaller.openSession(psid);

        for (int i = 1; i < vapkResIds.length; i++)
            addApkToSession(psession, getVapkStream(i), vapkBasenames[i] + ".apk");

        sessionQueue.add(psid);
    }

    private InputStream getApkStream(int i) {
        try {
            return openFileInput(apkBasenames[i] + ".apk");
        } catch (java.io.FileNotFoundException e) {
            return getResources().openRawResource(apkResIds[i]);
        }
    }

    private InputStream getVapkStream(int i) {
        try {
            return openFileInput(vapkBasenames[i] + ".apk");
        } catch (java.io.FileNotFoundException e) {
            return getResources().openRawResource(vapkResIds[i]);
        }
    }
}
