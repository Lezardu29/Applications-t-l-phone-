package com.filtrenotif.app;

import android.app.Notification;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FilterService extends NotificationListenerService {

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        SharedPreferences p = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE);
        String target = p.getString("pkg", MainActivity.DEFAULT_PKG).trim();
        if (!sbn.getPackageName().equals(target)) return;

        Bundle e = sbn.getNotification().extras;
        String content = join(
                e.getCharSequence(Notification.EXTRA_TITLE),
                e.getCharSequence(Notification.EXTRA_TEXT),
                e.getCharSequence(Notification.EXTRA_BIG_TEXT),
                e.getCharSequence(Notification.EXTRA_SUB_TEXT));
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
        addLog(p, (hide ? "[MASQUÉE] " : "[gardée] ") + content);
    }

    private static String join(CharSequence... parts) {
        StringBuilder sb = new StringBuilder();
        for (CharSequence c : parts) {
            if (c == null) continue;
            String s = c.toString().trim();
            if (s.isEmpty() || sb.indexOf(s) >= 0) continue;
            if (sb.length() > 0) sb.append(" | ");
            sb.append(s);
        }
        return sb.toString();
    }

    private static void addLog(SharedPreferences p, String line) {
        String time = new SimpleDateFormat("HH:mm", Locale.FRANCE).format(new Date());
        String old = p.getString("log", "");
        String[] lines = old.isEmpty() ? new String[0] : old.split("\n");
        StringBuilder sb = new StringBuilder(time).append(" ").append(line.replace("\n", " "));
        for (int i = 0; i < Math.min(lines.length, 29); i++) sb.append("\n").append(lines[i]);
        p.edit().putString("log", sb.toString()).apply();
    }
}
