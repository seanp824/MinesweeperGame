package com.example.minesweepergame;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {

    //  constants
    private static final int GRID_SIZE = 10;
    private static final int NUM_MINES = 6;
    private static final int REQUEST_RESULT = 1;

    // game state
    private boolean[][] mines = new boolean[GRID_SIZE][GRID_SIZE];
    private int[][] adjacentCounts = new int[GRID_SIZE][GRID_SIZE];
    private boolean[][] revealed = new boolean[GRID_SIZE][GRID_SIZE];
    private boolean[][] flagged = new boolean[GRID_SIZE][GRID_SIZE];
    private Button[][] cells = new Button[GRID_SIZE][GRID_SIZE];
    private boolean flagMode = false;
    private boolean gameOver = false;
    private boolean waitingForTap = false;
    private int secondsElapsed = 0;
    private boolean won = false;

    // ui
    private TextView tvMineCount, tvTimer, btnToggle;
    private GridLayout gridLayout;
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvMineCount = findViewById(R.id.tvMineCount);
        tvTimer = findViewById(R.id.tvTimer);
        btnToggle = findViewById(R.id.btnToggle);
        gridLayout = findViewById(R.id.gridLayout);

        // mode togle button
        ((Button) findViewById(R.id.btnToggle)).setOnClickListener(v -> {
            flagMode = !flagMode;
            ((Button) v).setText(flagMode ? "Mode: Flag 🚩" : "Mode: Dig ⛏");
        });

        // build grid after layout is measured
        gridLayout.post(() -> buildGrid());

        startTimer();
    }

    private void buildGrid() {
        int width = gridLayout.getWidth();
        int height = gridLayout.getHeight();
        int cellSize = Math.min(width, height) / GRID_SIZE;

        placeMines();
        calculateAdjacentCounts();

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                Button cell = new Button(this);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = cellSize;
                params.height = cellSize;
                params.rowSpec = GridLayout.spec(row);
                params.columnSpec = GridLayout.spec(col);
                params.setMargins(1, 1, 1, 1);
                cell.setLayoutParams(params);
                cell.setBackgroundColor(Color.parseColor("#4CAF50"));
                cell.setPadding(0, 0, 0, 0);
                cell.setTextSize(10f);

                final int r = row;
                final int c = col;
                cell.setOnClickListener(v -> onCellClick(r, c));

                cells[row][col] = cell;
                gridLayout.addView(cell);
            }
        }
    }

    private void placeMines() {
        // create list of all positions and shuffle
        ArrayList<Integer> positions = new ArrayList<>();
        for (int i = 0; i < GRID_SIZE * GRID_SIZE; i++) positions.add(i);
        Collections.shuffle(positions);

        for (int i = 0; i < NUM_MINES; i++) {
            int pos = positions.get(i);
            mines[pos / GRID_SIZE][pos % GRID_SIZE] = true;
        }
    }

    private void calculateAdjacentCounts() {
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                if (mines[r][c]) continue;
                int count = 0;
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        int nr = r + dr, nc = c + dc;
                        if (nr >= 0 && nr < GRID_SIZE && nc >= 0 && nc < GRID_SIZE && mines[nr][nc])
                            count++;
                    }
                }
                adjacentCounts[r][c] = count;
            }
        }
    }

    private void onCellClick(int row, int col) {
        // game over, next tap navigates to result
        if (waitingForTap) {
            goToResult();
            return;
        }
        if (gameOver) return;

        if (flagMode) {
            // Toggle flag
            if (!revealed[row][col]) {
                flagged[row][col] = !flagged[row][col];
                cells[row][col].setBackgroundColor(
                        flagged[row][col] ? Color.YELLOW : Color.parseColor("#4CAF50"));
                cells[row][col].setText(flagged[row][col] ? "🚩" : "");
                updateMineCount();
            }
        } else {
            // dig mode
            if (flagged[row][col]) return;
            if (revealed[row][col]) return;

            if (mines[row][col]) {
                //  mine hit
                cells[row][col].setBackgroundColor(Color.RED);
                cells[row][col].setText("💣");
                gameOver = true;
                won = false;
                stopTimer();
                waitingForTap = true;
            } else {
                revealCell(row, col);
                if (checkWin()) {
                    gameOver = true;
                    won = true;
                    stopTimer();
                    waitingForTap = true;
                }
            }
        }
    }

    private void revealCell(int row, int col) {
        if (row < 0 || row >= GRID_SIZE || col < 0 || col >= GRID_SIZE) return;
        if (revealed[row][col] || flagged[row][col] || mines[row][col]) return;

        revealed[row][col] = true;
        cells[row][col].setBackgroundColor(Color.WHITE);

        if (adjacentCounts[row][col] > 0) {
            cells[row][col].setText(String.valueOf(adjacentCounts[row][col]));
        } else {
            // flood fill, reveal all adjacent cells
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    revealCell(row + dr, col + dc);
                }
            }
        }
    }

    private boolean checkWin() {
        for (int r = 0; r < GRID_SIZE; r++)
            for (int c = 0; c < GRID_SIZE; c++)
                if (!mines[r][c] && !revealed[r][c]) return false;
        return true;
    }

    private void updateMineCount() {
        int flagCount = 0;
        for (int r = 0; r < GRID_SIZE; r++)
            for (int c = 0; c < GRID_SIZE; c++)
                if (flagged[r][c]) flagCount++;
        tvMineCount.setText("🚩 " + (NUM_MINES - flagCount));
    }

    private void startTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                secondsElapsed++;
                tvTimer.setText("⏱ " + secondsElapsed);
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private void stopTimer() {
        timerHandler.removeCallbacks(timerRunnable);
    }

    private void goToResult() {
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra("seconds", secondsElapsed);
        intent.putExtra("won", won);
        startActivityForResult(intent, REQUEST_RESULT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_RESULT && resultCode == RESULT_OK) {
            // reset
            recreate();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
    }
}