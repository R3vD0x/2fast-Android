package br.com.itisoft.a2fast;

import android.app.Application;

import br.com.itisoft.a2fast.data.AppPreferences;
import br.com.itisoft.a2fast.data.DatafileStorage;
import br.com.itisoft.a2fast.data.PasswordVault;

public class App extends Application {

    private static App instance;

    private AppPreferences preferences;
    private PasswordVault passwordVault;
    private DatafileStorage datafileStorage;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        preferences = new AppPreferences(this);
        passwordVault = new PasswordVault(this);
        datafileStorage = new DatafileStorage(this);
    }

    public static App get() {
        return instance;
    }

    public AppPreferences preferences() {
        return preferences;
    }

    public PasswordVault passwordVault() {
        return passwordVault;
    }

    public DatafileStorage datafileStorage() {
        return datafileStorage;
    }
}
