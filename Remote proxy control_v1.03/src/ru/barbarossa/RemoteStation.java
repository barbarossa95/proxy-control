package ru.barbarossa;

import javax.swing.JDialog;

/**
 *
 * @author Greshilov
 */
public class RemoteStation {
 
    private String password;
    private String username;
    private String hostname;
    
    public RemoteStation(String password, String username, String hostname) {
        this.password = password;
        this.username = username;
        this.hostname = hostname;
    }   

    public RemoteStation() {
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
}
