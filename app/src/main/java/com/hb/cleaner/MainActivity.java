package com.hb.cleaner;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.Formatter;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int REQ_STORAGE = 1;

    private TextView status;
    private Button scanBtn, cleanBtn;
    private Cleaner.Scan last;
    private boolean busy = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(48, 96, 48, 48);

        TextView title = new TextView(this);
        title.setTextSize(22);
        title.setText("HB Cleaner");

        TextView info = new TextView(this);
        info.setTextSize(14);
        info.setPadding(0, 24, 0, 24);
        info.setText("Cache, thumbnail ar temporary file clean kore. "
                + "Photo, video, document delete hoy na.");

        scanBtn = new Button(this);
        scanBtn.setText("Scan koro");
        scanBtn.setOnClickListener(v -> startScan());

        cleanBtn = new Button(this);
        cleanBtn.setText("Clean koro");
        cleanBtn.setEnabled(false);
        cleanBtn.setOnClickListener(v -> confirmClean());

        Button settingsBtn = new Button(this);
        settingsBtn.setText("Boro app er cache: Storage settings kholo");
        settingsBtn.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS));
            } catch (Exception e) {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            }
        });

        status = new TextView(this);
        status.setTextSize(16);
        status.setPadding(0, 32, 0, 0);
        status.setText("Prothome Scan koro.");

        root.addView(title);
        root.addView(info);
        root.addView(scanBtn);
        root.addView(cleanBtn);
        root.addView(settingsBtn);
        root.addView(status);

        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        setContentView(sv);
    }

    private boolean hasStoragePermission() {
        return checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == REQ_STORAGE) {
            if (hasStoragePermission()) {
                startScan();
            } else {
                status.setText("Storage permission na dile scan kora jabe na.");
            }
        }
    }

    private void startScan() {
        if (busy) return;
        if (!hasStoragePermission()) {
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_STORAGE);
            return;
        }
        busy = true;
        scanBtn.setEnabled(false);
        cleanBtn.setEnabled(false);
        status.setText("Scan cholche, ektu opekkha koro...");

        new Thread(() -> {
            Cleaner.Scan s = Cleaner.scan();
            runOnUiThread(() -> {
                last = s;
                busy = false;
                scanBtn.setEnabled(true);
                cleanBtn.setEnabled(s.count() > 0);
                status.setText("Scan shesh:\n\n"
                        + "App cache: " + size(s.cacheSize) + "\n"
                        + "Thumbnail: " + size(s.thumbSize) + "\n"
                        + "Temp/log file: " + size(s.tempSize) + "\n\n"
                        + "Mot: " + size(s.total()) + " (" + s.count() + " ta file)");
            });
        }).start();
    }

    private void confirmClean() {
        if (last == null || busy) return;
        new AlertDialog.Builder(this)
                .setTitle("Clean korbe?")
                .setMessage(size(last.total()) + " muche jabe. "
                        + "Photo, video, document delete hobe na.")
                .setNegativeButton("Na", null)
                .setPositiveButton("Haan, clean koro", (d, w) -> doClean())
                .show();
    }

    private void doClean() {
        final Cleaner.Scan s = last;
        busy = true;
        scanBtn.setEnabled(false);
        cleanBtn.setEnabled(false);
        status.setText("Clean cholche...");

        new Thread(() -> {
            long freed = Cleaner.delete(s.cache)
                    + Cleaner.delete(s.thumbs)
                    + Cleaner.delete(s.temp);
            freed += Cleaner.clearDir(getCacheDir());
            freed += Cleaner.clearDir(getExternalCacheDir());

            final long total = freed;
            runOnUiThread(() -> {
                busy = false;
                last = null;
                scanBtn.setEnabled(true);
                cleanBtn.setEnabled(false);
                status.setText("Clean shesh. " + size(total) + " jaygа khali holo.\n\n"
                        + "Boro app er internal cache muchte upor er "
                        + "\"Storage settings kholo\" button use koro.");
                Toast.makeText(this, size(total) + " clean holo", Toast.LENGTH_LONG).show();
            });
        }).start();
    }

    private String size(long bytes) {
        return Formatter.formatShortFileSize(this, bytes);
    }
}
