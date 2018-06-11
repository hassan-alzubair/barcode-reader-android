package com.hassan_alzubair.barcodereader;


import android.Manifest;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.zxing.Result;

import java.io.PrintWriter;
import java.net.Socket;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

/**
 * Created by Hassan on 11/10/2017.
 */

public class MainActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {


    private DrawerLayout mDrawer;
    private ActionBarDrawerToggle mToggle;
    private boolean started = false;
    private ZXingScannerView mScannerView;
    private boolean flash_on = false;
    private SharedPreferences preferences;
    private String ip, port;
    private static final int PERMISSION_CAMERA = 1;
    private android.support.v7.widget.Toolbar toolbar;
    private NavigationView mNavigationView;
    private CoordinatorLayout coordinator;

    private void testConnectionToComputer() {
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setTitle("Please Wait");
        dialog.setMessage("Connecting To Computer ...");
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setCancelable(false);
        dialog.show();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String ip = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getString("ip", "127.0.0.1");
                    final String port = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getString("port", "9090");

                    Socket s = new Socket(ip, Integer.parseInt(port));
                    dialog.dismiss();
                    s.close();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getBaseContext(), "Connection Successfull", Toast.LENGTH_LONG).show();
                        }
                    });

                } catch (Exception e) {
                    dialog.dismiss();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Connection Failed, Please Check Connection Settings", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
        thread.start();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Barcode Reader");
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mScannerView = findViewById(R.id.scanner);
        boolean autoFocus = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("focus", true);
        mScannerView.setAutoFocus(autoFocus);
        coordinator = findViewById(R.id.coordinator);


        mDrawer = findViewById(R.id.drawer);
        mToggle = new ActionBarDrawerToggle(this, mDrawer, toolbar, R.string.opened, R.string.closed);
        mDrawer.addDrawerListener(mToggle);
        mDrawer.post(new Runnable() {
            @Override
            public void run() {
                mToggle.syncState();
            }
        });

        mNavigationView = findViewById(R.id.navigation);
        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {

                if (item.getItemId() == R.id.textConnection) {
                    testConnectionToComputer();
                } else if (item.getItemId() == R.id.settings) {
                    startActivity(new Intent(getBaseContext(), SettingsActivity.class));
                } else if (item.getItemId() == R.id.about) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("About");
                    builder.setMessage("By Hassan Alzubair");
                    builder.show();
                } else if (item.getItemId() == R.id.start) {
                    if (started == false) {
                        if (isCameraPermissionGranted()) {
                            mScannerView.startCamera();
                            started = true;
                        } else {
                            showCameraPermissionDialog();
                        }
                    }
                } else if (item.getItemId() == R.id.stop) {
                    if (started == true) {
                        mScannerView.stopCamera();
                        started = false;
                    }
                } else if (item.getItemId() == R.id.flash) {
                    if (started) {
                        flash_on = !flash_on;
                        mScannerView.setFlash(flash_on);
                    }
                }


                // close drawer
                if (mDrawer.isDrawerOpen(Gravity.START))
                    mDrawer.closeDrawers();

                return true;
            }
        });
    }

    private boolean isCameraPermissionGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void showCameraPermissionDialog() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(coordinator, "Thank For Permission :)", Snackbar.LENGTH_SHORT).show();
                openCamera();
            } else {
                Toast.makeText(this, "Permission Denied , Cannot Start Barcode Scanner", Toast.LENGTH_SHORT).show();
            }
            return;
        }
    }

    private void openCamera() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (isCameraPermissionGranted()) {
                mScannerView.startCamera();
            } else {
                showCameraPermissionDialog();
            }
        } else {
            mScannerView.startCamera();
        }

    }


    @Override
    protected void onResume() {
        super.onResume();
        mScannerView.setResultHandler(this);
        if (started == true)
            openCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mScannerView.stopCamera();
    }

    @Override
    public void handleResult(Result result) {
        SendResult(result.getText());
        mScannerView.resumeCameraPreview(this);
    }

    private void Beep() {
        MediaPlayer.create(this, R.raw.beep).start();
    }

    private void SendResult(final String text) {

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String ip = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getString("ip", "127.0.0.1");
                    final String port = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getString("port", "9090");
                    Socket sck = new Socket(ip, Integer.parseInt(port));
                    PrintWriter wr = new PrintWriter(sck.getOutputStream(), true);
                    Log.e("Errrrrrrror", "run: " + text);
                    sendBarcode(wr, text);

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getBaseContext(), "Connection Failed With Computer , Please Check Connection Settings", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
        thread.start();
    }

    private void sendBarcode(PrintWriter wr, final String text) {

        final Snackbar snackbar = Snackbar.make(coordinator, text, Snackbar.LENGTH_LONG);
        snackbar.setAction("COPY", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardManager manager = (ClipboardManager) getBaseContext().getSystemService(CLIPBOARD_SERVICE);
                ClipData data = ClipData.newPlainText("barcode", text);
                manager.setPrimaryClip(data);
                snackbar.dismiss();
                Snackbar.make(coordinator, "Copied!", Snackbar.LENGTH_SHORT).show();
            }
        });
        snackbar.show();

        Beep();
        wr.println(text);
    }

}