package com.filtrenotif.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    static final String PREFS = "prefs";
    static final String DEFAULT_PKG = "com.snapchat.android";
    static final String DEFAULT_FILTERS = "envoyé un Snap";

    private SharedPreferences prefs;
    private TextView status;
    private TextView log;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);

        TextView title = new TextView(this);
        title.setText("Filtre de notifications");
        title.setTextSize(24);
        title.setTextColor(Color.WHITE);
        root.addView(title);

        status = new TextView(this);
        status.setTextSize(16);
        status.setPadding(0, dp(12), 0, dp(4));
        root.addView(status);

        Button access = new Button(this);
        access.setText("Ouvrir l'accès aux notifications");
        access.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        root.addView(access);

        Button battery = new Button(this);
        battery.setText("Désactiver l'optimisation de batterie");
        battery.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        android.net.Uri.parse("package:" + getPackageName())));
            } catch (Exception e) {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            }
        });
        root.addView(battery);

        Button autostart = new Button(this);
        autostart.setText("Démarrage automatique (Xiaomi / Poco)");
        autostart.setOnClickListener(v -> {
            try {
                Intent i = new Intent();
                i.setComponent(new android.content.ComponentName("com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"));
                startActivity(i);
            } catch (Exception e) {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        android.net.Uri.parse("package:" + getPackageName())));
            }
        });
        root.addView(autostart);

        Switch enabled = new Switch(this);
        enabled.setText("Filtre activé");
        enabled.setTextSize(16);
        enabled.setPadding(0, dp(16), 0, dp(8));
        enabled.setChecked(prefs.getBoolean("enabled", true));
        root.addView(enabled);

        root.addView(label("Application ciblée (nom du paquet) :"));
        EditText pkg = new EditText(this);
        pkg.setSingleLine(true);
        pkg.setText(prefs.getString("pkg", DEFAULT_PKG));
        root.addView(pkg);

        root.addView(label("Masquer les notifications qui contiennent "
                + "(un mot ou une phrase par ligne, majuscules ignorées) :"));
        EditText filters = new EditText(this);
        filters.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        filters.setMinLines(3);
        filters.setGravity(Gravity.TOP);
        filters.setText(prefs.getString("filters", DEFAULT_FILTERS));
        root.addView(filters);

        Button save = new Button(this);
        save.setText("Enregistrer");
        save.setOnClickListener(v -> {
            prefs.edit()
                    .putBoolean("enabled", enabled.isChecked())
                    .putString("pkg", pkg.getText().toString().trim())
                    .putString("filters", filters.getText().toString())
                    .apply();
            Toast.makeText(this, "Enregistré", Toast.LENGTH_SHORT).show();
        });
        root.addView(save);

        root.addView(label("Dernières notifications de l'app ciblée "
                + "(pour voir le texte exact à filtrer) :"));
        log = new TextView(this);
        log.setTextIsSelectable(true);
        log.setPadding(0, dp(8), 0, dp(8));
        root.addView(log);

        Button clear = new Button(this);
        clear.setText("Effacer l'historique");
        clear.setOnClickListener(v -> {
            prefs.edit().remove("log").apply();
            refresh();
        });
        root.addView(clear);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        String listeners = Settings.Secure.getString(getContentResolver(),
                "enabled_notification_listeners");
        boolean ok = listeners != null && listeners.contains(getPackageName());
        status.setText(ok ? "✅ Accès aux notifications autorisé"
                : "❌ Accès aux notifications non autorisé : appuie sur le bouton ci-dessous");
        status.setTextColor(ok ? Color.rgb(80, 200, 120) : Color.rgb(240, 90, 90));
        String l = prefs.getString("log", "");
        log.setText(l.isEmpty() ? "(rien pour l'instant)" : l);
    }

    private TextView label(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setPadding(0, dp(16), 0, 0);
        return t;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
