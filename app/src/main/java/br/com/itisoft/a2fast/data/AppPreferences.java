package br.com.itisoft.a2fast.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import br.com.itisoft.a2fast.model.DatafileEntry;

/**
 * App settings with support for multiple registered {@code .2fa} files.
 */
public final class AppPreferences {

    private static final String PREFS = "project2fa_settings";
    private static final String KEY_DATAFILES = "DataFilesJson";
    private static final String KEY_ACTIVE_ID = "ActiveDataFileId";
    private static final String KEY_HIDE_CODES = "UseHiddenTOTP";

    private static final Type LIST_TYPE = new TypeToken<List<DatafileEntry>>() {
    }.getType();

    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    public AppPreferences(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean hasConfiguredDatafile() {
        return !getDatafiles().isEmpty();
    }

    public List<DatafileEntry> getDatafiles() {
        String json = prefs.getString(KEY_DATAFILES, null);
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        List<DatafileEntry> list = gson.fromJson(json, LIST_TYPE);
        return list == null ? new ArrayList<>() : new ArrayList<>(list);
    }

    public DatafileEntry getDatafile(String id) {
        if (id == null) {
            return null;
        }
        for (DatafileEntry entry : getDatafiles()) {
            if (id.equals(entry.id)) {
                return entry;
            }
        }
        return null;
    }

    public DatafileEntry getActiveDatafile() {
        String activeId = getActiveDatafileId();
        if (activeId != null) {
            DatafileEntry active = getDatafile(activeId);
            if (active != null) {
                return active;
            }
        }
        List<DatafileEntry> all = getDatafiles();
        return all.isEmpty() ? null : all.get(0);
    }

    public String getActiveDatafileId() {
        return prefs.getString(KEY_ACTIVE_ID, null);
    }

    public void setActiveDatafileId(String id) {
        prefs.edit().putString(KEY_ACTIVE_ID, id).apply();
    }

    public DatafileEntry addOrUpdateDatafile(String displayName, String uri, String passwordHash) {
        List<DatafileEntry> list = getDatafiles();
        for (DatafileEntry entry : list) {
            if (uri != null && uri.equals(entry.uri)) {
                entry.displayName = displayName;
                entry.passwordHash = passwordHash;
                saveDatafiles(list);
                setActiveDatafileId(entry.id);
                return entry;
            }
        }
        DatafileEntry created = new DatafileEntry(displayName, uri, passwordHash);
        list.add(created);
        saveDatafiles(list);
        setActiveDatafileId(created.id);
        return created;
    }

    public void removeDatafile(String id) {
        List<DatafileEntry> list = getDatafiles();
        Iterator<DatafileEntry> it = list.iterator();
        while (it.hasNext()) {
            if (id.equals(it.next().id)) {
                it.remove();
                break;
            }
        }
        saveDatafiles(list);
        String active = getActiveDatafileId();
        if (id != null && id.equals(active)) {
            if (list.isEmpty()) {
                prefs.edit().remove(KEY_ACTIVE_ID).apply();
            } else {
                setActiveDatafileId(list.get(0).id);
            }
        }
    }

    public boolean useHiddenTotp() {
        return prefs.getBoolean(KEY_HIDE_CODES, false);
    }

    public void setUseHiddenTotp(boolean hide) {
        prefs.edit().putBoolean(KEY_HIDE_CODES, hide).apply();
    }

    private void saveDatafiles(List<DatafileEntry> list) {
        prefs.edit().putString(KEY_DATAFILES, gson.toJson(list == null ? Collections.emptyList() : list)).apply();
    }

}
