package ru.yandex.adeptsoftware.qiwibalancenotification;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.content.Intent;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        String str = "";
        int minute = seekBar.getProgress();
        int hour = 0;
        if (minute >= 60) {
            hour = minute/60;
            minute -= 60*hour;
            str += String.valueOf(hour)+ " ч. ";
        }
        if (minute != 0 && hour > 0) {
            if (minute >= 10) {
                str += String.valueOf(minute) + " мин.";
            }
            else {
                str += '0' + String.valueOf(minute) + " мин.";
            }
        }
        else {
            str += String.valueOf(minute) + " мин.";
        }
        Toast.makeText(MainActivity.this, str, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button btnStart = findViewById(R.id.start);
        final Button btnStop = findViewById(R.id.stop);
        final EditText edit = findViewById(R.id.editText);
        final EditText edit2 = findViewById(R.id.editText2);
        final SeekBar seek = findViewById(R.id.seekBar);

        seek.setOnSeekBarChangeListener(this);
        seek.setMax(60*24);

        SharedPreferences pr = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        edit.setText(pr.getString("token", ""));
        edit2.setText(pr.getString("number", ""));
        seek.setProgress(pr.getInt("update", 30));

        // запуск службы
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Editable strEdit = edit.getText();
                Editable strEdit2 = edit2.getText();
                int upd = seek.getProgress();
                if (upd <= 0) {
                    upd = 1;
                    seek.setProgress(1);
                    Toast.makeText(MainActivity.this, "Время обновление установлено на 1 мин!", Toast.LENGTH_SHORT).show();
                }
                if (!strEdit.toString().equals("") && !strEdit2.toString().equals("")) {
                    SharedPreferences pr = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    SharedPreferences.Editor ed = pr.edit();
                    ed.putInt("update", upd);
                    ed.putString("token", strEdit.toString());
                    ed.putString("number", strEdit2.toString());
                    ed.commit();
                    // используем явный вызов службы
                    startService(new Intent(MainActivity.this, QIWI_Service.class));
                }
                else
                    Toast.makeText(MainActivity.this, "Введите токен и номер кошелька!", Toast.LENGTH_SHORT).show();
            }
        });

        // остановка службы
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopService(new Intent(MainActivity.this, QIWI_Service.class));
            }
        });
    }
}