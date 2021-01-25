package egor_ind.apps.vibgyor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.security.Permission;
import java.util.Locale;
import java.util.Random;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "FragmentActivity";

    private MediaRecorder mediaRecorder;
    private TextView ampvalTV;
    private Button listenBtm;
    private ConstraintLayout constraintLayout;

    private boolean btnPressed;
    private final int AUDIO_REQUEST_CODE = 101;
    private final int AUDIO_RECORDING_DELAY = 200;
    private double lastAmp = 0;
    private double brightPercent;

    WindowManager.LayoutParams lp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ampvalTV = findViewById(R.id.ampVal);
        listenBtm = findViewById(R.id.listenBtn);
        constraintLayout = findViewById(R.id.mainContainer);

        lp = getWindow().getAttributes();
        btnPressed = true;
        brightPercent = 75;

        listenBtm.setEnabled(false);
        changeBrightness((int)brightPercent / (float)100);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, AUDIO_REQUEST_CODE);
        }

        listenBtm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnPressed) {
                    startRecording();
                    Toast.makeText(MainActivity.this, "listening", Toast.LENGTH_SHORT).show();
                    btnPressed = false;

                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            makeVibe();
                            handler.postDelayed(this, AUDIO_RECORDING_DELAY);
                        }
                    }, AUDIO_RECORDING_DELAY);
                } else {
                    stopRecording();
                    btnPressed = true;
                    Toast.makeText(MainActivity.this, "stopped listening", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == AUDIO_REQUEST_CODE) {
                listenBtm.setEnabled(true);
            } else {
                Toast.makeText(this, "No audio recording permissions given!", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        startRecording();
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopRecording();
    }

    private void startRecording() {
        mediaRecorder = new MediaRecorder();

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile("/dev/null");

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopRecording() {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

    private double getMaxAmplitude() {
        if (mediaRecorder != null) {
            final double maxAmplitude = mediaRecorder.getMaxAmplitude();

            return maxAmplitude;
        } else {
            return 0.0;
        }
    }

    private void changeBrightness(float brightness) {
        lp.screenBrightness = brightness;
        getWindow().setAttributes(lp);
    }

    private void makeVibe() {
        final double amplitude = getMaxAmplitude();
        if (amplitude != 0) {
            double ampRatio;
            if (lastAmp != 0) {
                ampRatio = amplitude / lastAmp;
                if (ampRatio > 1) {
                    ampRatio = ampRatio - 1;
                } else if (ampRatio < 1) {
                    ampRatio = -ampRatio;
                }
            } else {
                ampRatio = 0;
            }

            brightPercent = 75 + (25 * ampRatio);

            if (amplitude > lastAmp) {
                Random random = new Random();
                String[] colorArr = getResources().getStringArray(R.array.color_array);
                constraintLayout.setBackgroundColor(Color.parseColor(colorArr[random.nextInt(5)]));
            }

            if (brightPercent > 100) {
                brightPercent = 100;
            } else if (brightPercent < 50) {
                brightPercent = 50;
            }

            changeBrightness(Math.round(brightPercent) / (float)100);
            Log.d(TAG, "run: " + getWindow().getAttributes().screenBrightness + " " + brightPercent + " " + ampRatio);
            lastAmp = amplitude;
        }
        ampvalTV.setText(String.format(Locale.US, "%.2f", amplitude));
    }
}