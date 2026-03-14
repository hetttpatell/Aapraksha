package com.example.aapraksha;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class AlertDetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert_detail);

        // Example: set data from intent or static for now
        TextView userName = findViewById(R.id.alert_user_name);
        TextView time = findViewById(R.id.alert_time);
        Button respondNow = findViewById(R.id.btn_respond_now);
        Button viewLocation = findViewById(R.id.btn_view_location);
        Button caseResolved = findViewById(R.id.btn_case_resolved);

        // TODO: Set data from intent extras
        // userName.setText(...);
        // time.setText(...);
        // Show/hide caseResolved as needed
    }
}
