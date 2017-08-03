/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.barbarossa;

import java.io.File;

/**
 *
 * @author Greshilov
 */
public class RemoteControlWithAuth implements Runnable{

    public RemoteControlWithAuth(RemoteStation connection, File templateConfig, String port) {
        this.target = connection;
        this.configTemplate = templateConfig;
        this.port = port;
    }
    
    @Override
    public void run() {
        
    }
    
    /**
     * @return the target
     */
    public RemoteStation getTarget() {
        return target;
    }

    /**
     * @param target the target to set
     */
    public void setTarget(RemoteStation target) {
        this.target = target;
    }

    /**
     * @return the configTemplate
     */
    public File getConfigTemplate() {
        return configTemplate;
    }

    /**
     * @param configTemplate the configTemplate to set
     */
    public void setConfigTemplate(File configTemplate) {
        this.configTemplate = configTemplate;
    }

    /**
     * @return the port
     */
    public String getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(String port) {
        this.port = port;
    }

    private RemoteStation target;
    private File configTemplate;
    private String port;
}
