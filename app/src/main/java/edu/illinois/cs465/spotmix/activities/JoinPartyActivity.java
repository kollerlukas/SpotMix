package edu.illinois.cs465.spotmix.activities;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.InputType;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;

import edu.illinois.cs465.spotmix.R;
import edu.illinois.cs465.spotmix.api.firebase.FirebaseHelper;
import edu.illinois.cs465.spotmix.api.firebase.models.Attendee;
import edu.illinois.cs465.spotmix.api.firebase.models.Party;

public class JoinPartyActivity extends AppCompatActivity
        implements View.OnClickListener, FirebaseHelper.JoinCallback{
    BarcodeDetector barcodeDetector;
    CameraSource cameraSource;

    // Used to prevent multiple scans/popups
    boolean qrCodeScanned = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_party);

        EditText editText = findViewById(R.id.party_code_edit_txt);

        Button joinPartyBtn = findViewById(R.id.join_btn);

        SurfaceView surfaceView = findViewById(R.id.qrCodeScanner);

        barcodeDetector = new BarcodeDetector.Builder(JoinPartyActivity.this)
                .setBarcodeFormats(Barcode.QR_CODE).build();

        if (!barcodeDetector.isOperational()) {
            Toast.makeText(JoinPartyActivity.this, "Could not set up the detector!", Toast.LENGTH_SHORT).show();
        }

        cameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setRequestedPreviewSize(640, 480).build();

        ActivityCompat.requestPermissions(JoinPartyActivity.this, new String[]{Manifest.permission.CAMERA}, 50);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                try {
                    cameraSource.start(surfaceView.getHolder());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                cameraSource.stop();
            }
        });

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                SparseArray<Barcode> qrCodes = detections.getDetectedItems();

                if (qrCodes.size() != 0) {
                    if (!qrCodeScanned) {
                        qrCodeScanned = true;
                        editText.post(() -> {
                            editText.setText(qrCodes.valueAt(0).displayValue);
                            joinPartyBtn.post(joinPartyBtn::performClick);
                        });
                    }
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (v.getId()) {
            case R.id.join_btn:
                // get party code from edit text
                EditText editText = findViewById(R.id.party_code_edit_txt);
                final String partyCode = editText.getText().toString();
                // ask user for name with AlertDialog
                // create EditText to show in AlertDialog
                final EditText nameEditText = new EditText(this);
                nameEditText.setInputType(InputType.TYPE_CLASS_TEXT);
                // create AlertDialog
                new AlertDialog.Builder(this)
                        .setMessage("Enter your name:")
                        .setView(nameEditText)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // get name from dialog text
                                String attendeeName = nameEditText.getText().toString();
                                // request to join a party
                                new FirebaseHelper().joinParty(partyCode, attendeeName,
                                        JoinPartyActivity.this);
                            }
                        })
                        // listener null, because just dismissing the dialog, doing nothing else
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                break;
            default:
                break;
        }
    }

    @Override
    public void onPartyJoined(@org.jetbrains.annotations.Nullable Party party,
                              @org.jetbrains.annotations.Nullable Attendee attendee) {
        if (party != null && attendee != null) {
            // start party activity with party
            Intent intent = new Intent(this, PartyActivity.class);
            // pass party & attendee instance to party activity
            intent.putExtra(Party.PARCEL_KEY, party);
            intent.putExtra(Attendee.PARCEL_KEY, attendee);
            // start party activity
            startActivity(intent);
            // close Join activity
            finish();
        } else {
            // TODO: error handling
            Toast.makeText(this, "Some error...", Toast.LENGTH_SHORT).show();

            // In case qrCodeScanner scanned an invalid QR Code, reset qrCodeScanned so we can scan again
            qrCodeScanned = false;
        }
    }
}
