package com.example.ridenow;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ridenow.network.ApiClient;
import org.json.JSONObject;

public class MatchingResponseActivity extends AppCompatActivity {

    private TextView tvDriverName, tvDestination, tvOffer, tvTimer;
    private Button btnAccept, btnRefuse;
    private String rideId;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matching_response);

        rideId = getIntent().getStringExtra("ride_id");
        String driverName = getIntent().getStringExtra("driver_name");
        String destination = getIntent().getStringExtra("destination");
        double offer = getIntent().getDoubleExtra("offer", 0);

        tvDriverName = findViewById(R.id.tvDriverName);
        tvDestination = findViewById(R.id.tvDestination);
        tvOffer = findViewById(R.id.tvOffer);
        tvTimer = findViewById(R.id.tvTimer);
        btnAccept = findViewById(R.id.btnAccept);
        btnRefuse = findViewById(R.id.btnRefuse);

        tvDriverName.setText(driverName);
        tvDestination.setText(destination);
        tvOffer.setText(offer + " MAD");

        startTimer();

        btnAccept.setOnClickListener(v -> respondToMatching(true));
        btnRefuse.setOnClickListener(v -> respondToMatching(false));
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvTimer.setText((millisUntilFinished / 1000) + "s");
            }

            @Override
            public void onFinish() {
                tvTimer.setText("0s");
                respondToMatching(false);
            }
        }.start();
    }

    private void respondToMatching(boolean accept) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        ApiClient.respondToMatching(rideId, accept, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                if (accept) {
                    Toast.makeText(MatchingResponseActivity.this, "✅ Course acceptée !", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MatchingResponseActivity.this, "Course refusée", Toast.LENGTH_SHORT).show();
                }
                finish();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MatchingResponseActivity.this, "Erreur: " + error, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}