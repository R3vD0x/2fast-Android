package br.com.itisoft.a2fast.model;

import java.util.UUID;

/**
 * Registered {@code .2fa} datafile in the app (URI + unlock metadata).
 */
public class DatafileEntry {

    public String id;
    public String displayName;
    public String uri;
    public String passwordHash;

    public DatafileEntry() {
    }

    public DatafileEntry(String displayName, String uri, String passwordHash) {
        this.id = UUID.randomUUID().toString();
        this.displayName = displayName;
        this.uri = uri;
        this.passwordHash = passwordHash;
    }
}
