package ru.yandex.adeptsoftware.qiwibalancenotification;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import android.widget.Toast;
import android.support.v4.app.NotificationCompat;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;


public class QIWI_Service extends Service {

    Thread m_thread = null;
    private boolean m_bStart = false;

    public QIWI_Service() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        if (m_bStart) {
            m_bStart = false;
            m_thread.interrupt();
            m_thread = null;
            super.onDestroy();
            Toast.makeText(this, "Служба остановлена", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!m_bStart) {
            Toast.makeText(this, "Служба запущена", Toast.LENGTH_SHORT).show();
            m_bStart = true;
            m_thread = new RunThread(this);
            m_thread.setDaemon(true);
            m_thread.start();
        }
        return Service.START_STICKY;
    }

    class RunThread extends Thread {
        private String m_strToken;
        private String m_strNumber;
        private int m_time_update;
        private QIWI_Service m_pService;

        private RunThread(QIWI_Service q) {
            m_pService = q;
        }

        public void run() {
            SharedPreferences pr = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            m_strToken = pr.getString("token", "");
            m_strNumber = pr.getString("number", "");
            m_time_update = pr.getInt("update", 1)*60000;
            String strUrl = "https://edge.qiwi.com/funding-sources/v2/persons/"+m_strNumber+"/accounts";
            Date oldDate = new Date(0,0,0);
            while (!isInterrupted()) {
                Date d = new Date();
                if (d.getTime() - oldDate.getTime() > m_time_update) {
                    try {
                        URL url = new URL(strUrl);
                        HttpURLConnection con = (HttpURLConnection) url.openConnection();
                        con.setRequestProperty("Authorization", "Bearer " + m_strToken);
                        int res = con.getResponseCode();
                        if (res == HttpURLConnection.HTTP_OK) {
                            InputStream obj = con.getInputStream();
                            BufferedReader reader = new BufferedReader(new InputStreamReader(obj, "UTF-8"), 8);
                            StringBuilder sb = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null)
                            {
                                line += '\n';
                                sb.append(line);
                            }
                            JSONObject json = new JSONObject(sb.toString());
                            JSONArray accounts = json.getJSONArray("accounts");
                            for (int i=0; i<accounts.length(); i++){
                                JSONObject object = accounts.getJSONObject(i);
                                if (object.has("balance") && !object.isNull("balance")){
                                    JSONObject balance = object.getJSONObject("balance");
                                    int money = balance.getInt("amount");
                                    String title = object.getString("title");
                                    int current_value = pr.getInt(title, -1000000);
                                    if (current_value == -1000000 || money != current_value){
                                        SharedPreferences.Editor ed = pr.edit();
                                        ed.putInt(title, money);
                                        ed.commit();
                                    }
                                    if (money != current_value) {
                                        if (current_value == -1000000) {
                                            Notify("Текущий счет: " + String.valueOf(money));
                                        }
                                        else {
                                            int dx = money - current_value;
                                            Notify("Поступило: " + String.valueOf(dx) + ". Счет составляет: " + String.valueOf(money));
                                        }
                                    }
                                }
                            }
                            oldDate = new Date();
                        } else {
                            Notify("Убедитесь, если ли интернет, тот ли номер кошелька, токен не истек? (180 дней)!");
                            m_pService.stopSelf();
                        }
                    } catch (Exception e) {
                        Notify(e.toString());
                        e.printStackTrace();
                        break;
                    }
                }
            }
        }

        private void Notify(String text) {
            /*Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
            PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                    notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);*/
            NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());

            // builder.setContentIntent(contentIntent);
            builder.setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle("QIWI Balance notification")
                    .setContentText(text)
                    .setWhen(System.currentTimeMillis())
                    .setAutoCancel(true); // автоматически закрыть уведомление после нажатия

            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(125, builder.build());
        }

    }
}