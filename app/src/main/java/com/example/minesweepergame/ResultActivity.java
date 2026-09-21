package com.example.minesweepergame;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        TextView tvResult = findViewById(R.id.tvResult);
        Button btnPlayAgain = findViewById(R.id.btnPlayAgain);

        // get result data
        int seconds = getIntent().getIntExtra("seconds", 0);
        boolean won = getIntent().getBooleanExtra("won", false);

        // display result
        if (won) {
            tvResult.setText("Used " + seconds + " seconds.\nYou won.\nGood job!");
        } else {
            tvResult.setText("Used " + seconds + " seconds.\nYou lost.\nBetter luck next time!");
        }

        // play again returns to mainactivity
        btnPlayAgain.setOnClickListener(v -> {
            setResult(RESULT_OK);
            finish();
        });
    }
}