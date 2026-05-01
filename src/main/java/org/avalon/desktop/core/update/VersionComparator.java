package org.avalon.desktop.core.update;

public class VersionComparator {
    public static boolean isNewer(String currentVersion, String latestVersion) {
        if (latestVersion == null || currentVersion == null) return false;
        
        String[] current = currentVersion.replaceAll("[^0-9.]", "").split("\\.");
        String[] latest = latestVersion.replaceAll("[^0-9.]", "").split("\\.");
        
        int length = Math.max(current.length, latest.length);
        for (int i = 0; i < length; i++) {
            int v1 = i < current.length ? Integer.parseInt(current[i]) : 0;
            int v2 = i < latest.length ? Integer.parseInt(latest[i]) : 0;
            if (v2 > v1) return true;
            if (v2 < v1) return false;
        }
        return false;
    }
}
