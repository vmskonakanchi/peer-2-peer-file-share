package com.vamsik.core;

import javafx.concurrent.Task;
import com.vamsik.utils.FileUtils;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ClientHandler {
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());

    // to remember peer info from QUERY
    private String lastKnownHost;
    private int lastKnownPort;
    private String lastKnownFile;
    private final String sharedDirectory;

    private final String discoveryHost;
    private final int discoveryPort;

    public ClientHandler(String dirName) {
        this.sharedDirectory = dirName;
        this.discoveryHost = "localhost";
        this.discoveryPort = 3969;
    }

    public ClientHandler(String discoveryHost, int discoveryPort, String dirName) {
        this.discoveryHost = discoveryHost;
        this.discoveryPort = discoveryPort;
        this.sharedDirectory = dirName;
    }

    private String getDiscoveryHost() {
        return discoveryHost;
    }

    private int getDiscoveryPort() {
        return discoveryPort;
    }

    public Task<List<String>> getAllFiles() {
        return new Task<>() {
            @Override
            protected List<String> call() {
                try (Socket discoverySocket = new Socket(getDiscoveryHost(), getDiscoveryPort());
                     DataOutputStream dos = new DataOutputStream(discoverySocket.getOutputStream());
                     DataInputStream dis = new DataInputStream(discoverySocket.getInputStream())) {

                    logger.info("Connected to Discovery Service");

                    dos.writeUTF("LIST");
                    dos.flush();

                    int resultSize = dis.readInt();
                    logger.info("Discovery Service Response: " + resultSize + " files found.");

                    List<String> fileList = new ArrayList<>();

                    for (int i = 0; i < resultSize; i++) {
                        String[] fileNames = dis.readUTF().split(",");
                        for (String fileName : fileNames) {
                            fileName = fileName.trim();
                            if (!fileName.isEmpty()) {
                                fileList.add(fileName);
                                logger.info("File: " + fileName);
                            }
                        }
                    }

                    return fileList;
                } catch (EOFException | SocketException e) {
                    logger.info("Discovery service is not running. Please start it first.");
                    return List.of();
                } catch (Exception e) {
                    e.printStackTrace();
                    return List.of();
                }
            }
        };
    }

    public Task<String> checkFileExist(String fileName) {
        return new Task<>() {
            @Override
            protected String call() {
                try (Socket discoverySocket = new Socket(getDiscoveryHost(), getDiscoveryPort());
                     DataOutputStream dos = new DataOutputStream(discoverySocket.getOutputStream());
                     DataInputStream dis = new DataInputStream(discoverySocket.getInputStream())) {

                    logger.info("Connected to Discovery Service");

                    dos.writeUTF("QUERY");
                    dos.writeUTF(fileName);
                    dos.flush();

                    String response = dis.readUTF();
                    logger.info("Discovery Service Response: " + response);
                    int hostPorts = dis.readInt();

                    for (int i = 0; i < hostPorts; i++) {
                        String hostPort = dis.readUTF();
                        String[] parts = hostPort.split(":");
                        lastKnownHost = parts[0];
                        lastKnownPort = Integer.parseInt(parts[1]);
                    }

                    lastKnownFile = fileName;

                    return response;
                } catch (EOFException | SocketException e) {
                    logger.info("Discovery service is not running. Please start it first.");
                    return "NOTFOUND";
                } catch (Exception e) {
                    e.printStackTrace();
                    return "ERROR";
                }
            }
        };
    }

    public Task<Boolean> downloadFile(String fileName) {
        return new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                if (lastKnownHost == null || lastKnownFile == null) {
                    logger.info("No peer information available. Please use QUERY first.");
                    return false;
                }

                try (Socket peerSocket = new Socket(lastKnownHost, lastKnownPort);
                     DataOutputStream dos = new DataOutputStream(peerSocket.getOutputStream());
                     DataInputStream dis = new DataInputStream(peerSocket.getInputStream())
                ) {
                    dos.writeUTF("DOYOUHAVE");
                    dos.writeUTF(fileName);
                    dos.flush();

                    String response = dis.readUTF();

                    boolean hasFound = response.equalsIgnoreCase("YES");

                    if (!hasFound) {
                        return false;
                    }

                    dos.writeUTF("DOWNLOAD");
                    dos.writeUTF(fileName);
                    dos.flush();

                    String gotFileName = dis.readUTF();
                    long fileSize = dis.readLong();

                    try (FileOutputStream fos = new FileOutputStream(FileUtils.getFullPath(sharedDirectory, "received_" + gotFileName))) {
                        byte[] buffer = new byte[1024]; // Smaller buffer for more frequent updates
                        int bytesRead;

                        logger.info("Downloading file: " + gotFileName + " of size: " + fileSize + " bytes");

                        while ((bytesRead = dis.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }

                        fos.flush(); // Ensure all data is written to the file
                    }

                    logger.info("File downloaded successfully: received_" + fileName);

                    return true;
                } catch (EOFException | SocketException e) {
                    logger.info("Peer is not available. Please use QUERY to find another peer.");
                    return false;
                } catch (Exception e) {
                    return false;
                }
            }
        };
    }
}
