package com.filtrenotif.app;

import android.app.Notification;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FilterService extends NotificationListenerService {

    private final Handler handler = new Handler(Looper.getMainLooper());
    // Évite d'écrire plusieurs fois la même notification dans l'historique.
    private final Map<String, String> lastLogged = new HashMap<>();

    @Override
    public void onListenerConnected() {
        // Au (re)démarrage du service, on nettoie ce qui est déjà affiché.
        scanAll();
    }

    @Override
    public void onListenerDisconnected() {
        // HyperOS coupe parfois le service : on demande à Android de le relancer.
        requestRebind(new ComponentName(this, FilterService.class));
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (!isTarget(sbn)) return;
        process(sbn);
        // Snapchat met parfois à jour sa notification juste après :
        // on repasse plusieurs fois pour rattraper ces mises à jour.
        handler.postDelayed(this::scanAll, 400);
        handler.postDelayed(this::scanAll, 1500);
    }

    private void scanAll() {
        try {
            StatusBarNotification[] active = getActiveNotifications();
            if (active == null) return;
            for (StatusBarNotification sbn : active) {
                if (isTarget(sbn)) process(sbn);
            }
        } catch (Exception ignored) {
            // Le service n'est pas encore (ou plus) connecté.
        }
    }

    private SharedPreferences prefs() {
        return getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE);
    }

    private boolean isTarget(StatusBarNotification sbn) {
        String target = prefs().getString("pkg", MainActivity.DEFAULT_PKG).trim();
        return sbn.getPackageName().equals(target);
    }

    private void process(StatusBarNotification sbn) {
        SharedPreferences p = prefs();
        String content = extractText(sbn.getNotification());
        if (content.isEmpty()) return;

        boolean hide = false;
        if (p.getBoolean("enabled", true)) {
            String lower = content.toLowerCase(Locale.ROOT);
            for (String f : p.getString("filters", MainActivity.DEFAULT_FILTERS).split("\n")) {
                String t = f.trim().toLowerCase(Locale.ROOT);
                if (!t.isEmpty() && lower.contains(t)) {
                    hide = true;
                    break;
                }
            }
        }

        if (hide) cancelNotification(sbn.getKey());

        String entry = (hide ? "[MASQUÉE] " : "[gardée] ") + content;
        if (!entry.equals(lastLogged.get(sbn.getKey()))) {
            lastLogged.put(sbn.getKey(), entry);
            if (lastLogged.size() > 200) lastLogged.clear();
            addLog(p, entry);
        }
    }

    /** Récupère tout le texte visible d'une notification, quel que soit son style. */
    private static String extractText(Notification n) {
        Bundle e = n.extras;
        StringBuilder sb = new StringBuilder();
        add(sb, e.getCharSequence(Notification.EXTRA_TITLE));
        add(sb, e.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE));
        add(sb, e.getCharSequence(Notification.EXTRA_TEXT));
        add(sb, e.getCharSequence(Notification.EXTRA_BIG_TEXT));
        add(sb, e.getCharSequence(Notification.EXTRA_SUB_TEXT));
        add(sb, e.getCharSequence(Notification.EXTRA_SUMMARY_TEXT));
        add(sb, n.tickerText);

        CharSequence[] lines = e.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        if (lines != null) for (CharSequence l : lines) add(sb, l);

        Parcelable[] messages = e.getParcelableArray(Notification.EXTRA_MESSAGES);
        if (messages != null) {
            for (Parcelable m : messages) {
                if (m instanceof Bundle) add(sb, ((Bundle) m).getCharSequence("text"));
            }
        }
        return sb.toString();
    }

    private static void add(StringBuilder sb, CharSequence c) {
        if (c == null) return;
        String s = c.toString().trim().replace("\n", " ");
        if (s.isEmpty() || sb.indexOf(s) >= 0) return;
        if (sb.length() > 0) sb.append(" | ");
        sb.append(s);
    }

    private static void addLog(SharedPreferences p, String line) {
        String time = new SimpleDateFormat("HH:mm:ss", Locale.FRANCE).format(new Date());
        String old = p.getString("log", "");
        String[] lines = old.isEmpty() ? new String[0] : old.split("\n");
        StringBuilder sb = new StringBuilder(time).append(" ").append(line);
        for (int i = 0; i < Math.min(lines.length, 39); i++) sb.append("\n").append(lines[i]);
        p.edit().putString("log", sb.toString()).apply();
    }
}
