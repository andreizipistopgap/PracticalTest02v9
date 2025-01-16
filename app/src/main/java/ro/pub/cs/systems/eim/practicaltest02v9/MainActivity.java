package ro.pub.cs.systems.eim.practicaltest02v9;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "PracticalTest02V9";
    private AnagramReceiver anagramReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Ajustare padding pentru a lucra corect cu system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Legăm componentele UI
        TextView resultTextView = findViewById(R.id.resultTextView);
        EditText wordInput = findViewById(R.id.wordInput);
        EditText minLengthInput = findViewById(R.id.minLengthInput);
        Button searchButton = findViewById(R.id.searchButton);

        // Înregistrăm receiver-ul pentru broadcast
        anagramReceiver = new AnagramReceiver(resultTextView);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(anagramReceiver, new IntentFilter("ro.pub.cs.systems.eim.practicaltest02v9.ANAGRAMS"), Context.RECEIVER_NOT_EXPORTED);
        }

        // Permite operațiuni de rețea pe thread-ul principal (doar pentru testare, nu în producție!)
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // Setăm logica butonului pentru a iniția cererea HTTP
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String word = wordInput.getText().toString().trim();
                String minLengthStr = minLengthInput.getText().toString().trim();

                if (word.isEmpty() || minLengthStr.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Introduceți un cuvânt și o dimensiune minimă!", Toast.LENGTH_SHORT).show();
                    return;
                }

                int minLength;
                try {
                    minLength = Integer.parseInt(minLengthStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(MainActivity.this, "Dimensiunea minimă trebuie să fie un număr valid!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Apelează metoda pentru cererea HTTP
                fetchAnagrams(word, minLength);
            }
        });
    }

    private void fetchAnagrams(String word, int minLength) {
        try {
            String urlString = "http://anagramica.com/all/" + word;
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) { // OK
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // Afișează răspunsul complet în LogCat
                Log.d(TAG, "Răspuns complet: " + response.toString());

                // Parsarea obiectului JSON
                JSONObject jsonResponse = new JSONObject(response.toString());
                JSONArray anagrams = jsonResponse.getJSONArray("all");
                StringBuilder parsedAnagrams = new StringBuilder();
                for (int i = 0; i < anagrams.length(); i++) {
                    String anagram = anagrams.getString(i);
                    if (anagram.length() >= minLength) {
                        parsedAnagrams.append(anagram).append(", ");
                    }
                }

                // Afișează anagramele parsate în LogCat
                Log.d(TAG, "Anagrame parsate: " + parsedAnagrams.toString());

                // Trimiterea datelor prin Broadcast
                Intent broadcastIntent = new Intent("ro.pub.cs.systems.eim.practicaltest02v9.ANAGRAMS");
                broadcastIntent.putExtra("anagrams", parsedAnagrams.toString());
                sendBroadcast(broadcastIntent);

            } else {
                Log.e(TAG, "Cod de răspuns HTTP: " + responseCode);
                Toast.makeText(this, "Eroare la conectarea la server", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Eroare: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(anagramReceiver);
    }
}
