package com.vamsik;

public class Entry {
    private String host;
    private int port;
    private String[] files;
    private String cpuName;

    public Entry(String host, int port, String[] files, String cpuName) {
        this.host = host;
        this.port = port;
        this.files = files;
        this.cpuName = cpuName;
    }

    public Entry(String host, int port, String cpuName) {
        this.host = host;
        this.port = port;
        this.files = new String[0];
        this.cpuName = cpuName;
    }

    public Entry(String host, int port) {
        this.host = host;
        this.port = port;
        this.files = new String[0];
    }

    public boolean hasFile(String filename) {
        for (String file : files) {
            if (file.trim().equalsIgnoreCase(filename.trim())) {
                return true;
            }
        }
        return false;
    }

    public void addFile(String filename) {
        String[] newFiles = new String[files.length + 1];
        System.arraycopy(files, 0, newFiles, 0, files.length);
        newFiles[files.length] = filename;
        files = newFiles;
    }

    public String getFile() {
        StringBuilder sb = new StringBuilder();
        for (String file : files) {
            sb.append(file).append(",");
        }
        return sb.toString().trim();
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getCpuName() {
        return cpuName;
    }

    @Override
    public String toString() {
        return this.getHost() + ":" + this.getPort() + ":" + this.getCpuName();
    }
}
