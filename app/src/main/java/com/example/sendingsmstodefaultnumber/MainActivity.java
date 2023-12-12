package com.example.sendingsmstodefaultnumber;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.location.Location;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity {

    final int SEND_SMS_PERMISSION_REQUEST_CODE = 1;
    final int CALL_PHONE_PERMISSION_REQUEST_CODE = 1;
    private static final int REQUEST_CODE_LOCATION_PERMISSION = 1;

    FloatingActionButton send;
    FloatingActionButton call;
    FloatingActionButton location;
    FloatingActionButton getHelp;
    private TextView textLatLong;
    private TextView textAddress;
    private ProgressBar progressBar;
    private ResultReceiver resultReceiver;

    private CameraManager cameraManager;
    private String cameraID;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraID = cameraManager.getCameraIdList()[0];
        } catch (Exception e) {
            e.printStackTrace();
        }

        send = findViewById(R.id.floatingActionButtonMsg);
        call = findViewById(R.id.floatingActionButtonPhone);
        location = findViewById(R.id.floatingActionButtonLocation);
        getHelp = findViewById(R.id.floatingActionButtongetHelp);

        resultReceiver = new AddressResultReceiver(new Handler());

        textLatLong = findViewById(R.id.textLatLong);
        textAddress = findViewById(R.id.textAddress);
        progressBar = findViewById(R.id.progressBar);

        send.setEnabled(false);
        if (checkPermission(Manifest.permission.SEND_SMS)) {
            send.setEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SEND_SMS_PERMISSION_REQUEST_CODE);
        }

        call.setEnabled(false);
        if (checkPermission(Manifest.permission.CALL_PHONE)) {
            call.setEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, CALL_PHONE_PERMISSION_REQUEST_CODE);
        }

        location.setEnabled(false);
        if (checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            location.setEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION_PERMISSION);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void onTorchOn(View view) {
        try {
            cameraManager.setTorchMode(cameraID, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void onTorchOff(View view) {
        try {
            cameraManager.setTorchMode(cameraID, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onGetHelp(View view) {
        getHelp.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(MainActivity.this, "Help!", Toast.LENGTH_SHORT).show();

//              Sending Message
                if (checkPermission(Manifest.permission.SEND_SMS)) {
                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage("+959444771857", null, textAddress.getText().toString(), null, null);
                    //smsManager.sendTextMessage("+9595107310", null, textAddress.getText().toString(), null, null);
                    //smsManager.sendTextMessage("+95973225150", null, textAddress.getText().toString(), null, null);
                }

//              Phone Calling
                final int REQUEST_PHONE_CALL = 1;
                if (checkPermission(Manifest.permission.CALL_PHONE)) {
                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                    callIntent.setData(Uri.parse("tel: +959444771857"));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CALL_PHONE}, REQUEST_PHONE_CALL);
                        } else {
                            startActivity(callIntent);
                        }
                    }
                }

                return false;
            }
        });
    }

    public void onLocation(View view) {
        findViewById(R.id.floatingActionButtonLocation).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                            MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION_PERMISSION
                    );
                } else {
                    getCurrentLocation();
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        } else {
            Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show();
        }
    }

    private void getCurrentLocation() {
        progressBar.setVisibility(View.VISIBLE);

        final LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.getFusedLocationProviderClient(MainActivity.this).requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                LocationServices.getFusedLocationProviderClient(MainActivity.this).removeLocationUpdates(this);
                if (locationRequest != null && locationResult.getLocations().size() > 0) {
                    int latestLocationIndex = locationResult.getLocations().size() - 1;
                    double latitude = locationResult.getLocations().get(latestLocationIndex).getLatitude();
                    double longitude = locationResult.getLocations().get(latestLocationIndex).getLongitude();
                    textLatLong.setText(
                            String.format(
                                    "Latitude: %s\nLongitude: %s",
                                    latitude,
                                    longitude
                            )
                    );
                    Location location = new Location("providerNA");
                    location.setLatitude(latitude);
                    location.setLongitude(longitude);
                    fetchAddressFromLatLong(location);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }
        }, Looper.getMainLooper());
    }

    private void fetchAddressFromLatLong(Location location)
    {
        Intent intent = new Intent(this, FetchAddressIntentService.class);
        intent.putExtra(Constants.RECEIVER, resultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, location);
        startService(intent);
    }

    private class AddressResultReceiver extends ResultReceiver
    {
        AddressResultReceiver(Handler handler)
        {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData)
        {
            super.onReceiveResult(resultCode, resultData);
            if(resultCode == Constants.SUCCESS_RESULT)
            {
                textAddress.setText(resultData.getString(Constants.RESULT_DATA_KEY));
            }
            else
            {
                Toast.makeText(MainActivity.this, resultData.getString(Constants.RESULT_DATA_KEY), Toast.LENGTH_SHORT).show();
            }
            progressBar.setVisibility(View.GONE);
        }
    }

    public void onCall(View vi)
    {
        final int REQUEST_PHONE_CALL = 1;
        if(checkPermission(Manifest.permission.CALL_PHONE))
        {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel: +959444771857"));
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CALL_PHONE}, REQUEST_PHONE_CALL);
                } else
                {
                    startActivity(callIntent);
                }
            }
        }
    }

    public void onSend(View v)
    {
        if (checkPermission(Manifest.permission.SEND_SMS))
        {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage("+959444771857", null, textAddress.getText().toString(), null, null );
            //smsManager.sendTextMessage("+9595107310", null, textAddress.getText().toString(), null, null);
            //smsManager.sendTextMessage("+95973225150", null, textAddress.getText().toString(), null, null);
            Toast.makeText(this, "Message Sent!", Toast.LENGTH_SHORT).show();
        }
        else
        {
            Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean checkPermission(String permission)
    {
        int check = ContextCompat.checkSelfPermission(this, permission);
        return (check == PackageManager.PERMISSION_GRANTED);
    }
}
