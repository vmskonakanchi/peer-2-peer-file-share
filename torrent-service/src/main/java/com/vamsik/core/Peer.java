package com.vamsik.core;

import com.vamsik.utils.Config;
import com.vamsik.utils.FileUtils;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class Peer extends Thread {
    private static final Logger logger = Logger.getLogger(Peer.class.getName());
    private final String discoveryHost;
    private final int discoveryPort;

    private final int serverPort;
    private final String sharedDirectory;

    private ServerSocket server;
    private volatile boolean running = true;

    public Peer(String disHost, int disPort, int port, String dirName) {
        this.discoveryHost = disHost;
        this.discoveryPort = disPort;
        this.serverPort = port;
        this.sharedDirectory = dirName;
        logger.info("Peer initialized with port: " + this.serverPort + " and directory: " + this.sharedDirectory);
    }

    public Peer(String disHost, int disPort) {
        this.discoveryHost = disHost;
        this.discoveryPort = disPort;
        this.serverPort = 8000;
        this.sharedDirectory = ".shared";
        logger.info("Peer initialized with port: " + this.serverPort + " and directory: " + this.sharedDirectory);
    }

    public Peer(int port, String dirName) {
        this.discoveryHost = "localhost";
        this.discoveryPort = 3969;
        this.serverPort = port;
        this.sharedDirectory = dirName;
        logger.info("Peer initialized with port: " + this.serverPort + " and directory: " + this.sharedDirectory);
    }

    public Peer() {
        this.serverPort = 8000;
        this.sharedDirectory = ".shared";
        this.discoveryHost = "localhost";
        this.discoveryPort = 3969;
        logger.info("Peer initialized with port: " + this.serverPort + " and directory: " + this.sharedDirectory);
    }

    public void disconnect() {
        running = false;
        try {
            if (server != null && !server.isClosed()) {
                server.close();  // This will cause server.accept() to throw an exception
            }
            logger.info("Disconnecting from peer...");
            this.interrupt();  // Interrupt the thread if it's blocked elsewhere
            this.join(1000);   // Wait for thread to finish
        } catch (InterruptedException e) {
            logger.info("Interrupted while disconnecting");
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            logger.info("Error closing server socket: " + e.getMessage());
        }
    }

    private void registerWithDiscovery() {
        try (Socket sock = new Socket(this.discoveryHost, this.discoveryPort);
             DataOutputStream dos = new DataOutputStream(sock.getOutputStream());
             DataInputStream dis = new DataInputStream(sock.getInputStream())
        ) {

            logger.info("Connected to Discovery Service");

            dos.writeUTF("REGISTER");

            if (Config.IS_DEBUG) {
                dos.writeUTF(sock.getInetAddress().getHostAddress());
            } else {
                dos.writeUTF(InetAddress.getLocalHost().getHostAddress());
            }

            dos.writeInt(this.serverPort);

            String[] files = FileUtils.walkDirectory(sharedDirectory);
            dos.writeInt(files.length); // send the length of the file list

            for (String file : files) {
                dos.writeUTF(file);
            }

            dos.flush();

            String response = dis.readUTF();
            logger.info("Discovery Service Response: " + response);
        } catch (EOFException | SocketException e) {
            logger.info("Discovery service is not running. Please start it first.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        // creating directory if not exists
        FileUtils.createFolder(sharedDirectory);

        // for handling clients
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        executorService.execute(this::registerWithDiscovery);

        try {
            server = new ServerSocket(this.serverPort);
            logger.info("Peer server started on " + this.serverPort);

            while (running) {
                try {
                    Socket sock = server.accept();
                    logger.info("Peer connected from " + sock.getInetAddress());
                    executorService.execute(() -> handleMessages(sock));
                } catch (IOException e) {
                    if (running) {
                        e.printStackTrace();
                    } else {
                        // This is expected when we close the socket
                        logger.info("Server socket closed");
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            executorService.shutdown();
        }
    }

    private void handleMessages(Socket sock) {

        try (DataInputStream ds = new DataInputStream(sock.getInputStream());
             DataOutputStream dos = new DataOutputStream(sock.getOutputStream())) {

            label:
            while (true) {
                String command = ds.readUTF();

                switch (command) {
                    case "DISCONNECT":
                        this.sendMessage(dos, "BYE");
                        break label;
                    case "DOYOUHAVE": {
                        String fileName = ds.readUTF();
                        boolean hasFile = FileUtils.checkFile(sharedDirectory, fileName);
                        this.sendMessage(dos, hasFile ? "YES" : "NO");
                        break;
                    }
                    case "DOWNLOAD": {
                        String fileName = ds.readUTF();
                        this.sendFile(dos, fileName);
                        break;
                    }
                    case "LIST": {
                        this.handleAllFilesRequest(dos);
                        break;
                    }
                    default:
                        this.sendMessage(dos, "OK");
                        break;
                }
            }

            sock.close(); // closing the socket after handling messages
        } catch (SocketException | EOFException e) {
            logger.info("Peer disconnected");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                sock.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handleAllFilesRequest(DataOutputStream dos) {
        try {
            String[] files = FileUtils.walkDirectory(sharedDirectory);
            dos.writeInt(files.length); // send the length of the file list

            for (String file : files) {
                dos.writeUTF(file);
            }

            dos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(DataOutputStream dos, String message) {
        try {
            dos.writeUTF(message);
            dos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendFile(DataOutputStream dos, String fileName) {
        try {
            String fullPath = FileUtils.getFullPath(sharedDirectory, fileName);
            long fileSize = FileUtils.getFileSize(sharedDirectory, fileName);

            // send the filename and fileSize first
            dos.writeUTF(fileName);
            dos.writeLong(fileSize);
            dos.flush();

            try (FileInputStream fileInputStream = new FileInputStream(fullPath)) {
                logger.info("Sending file: " + fileName + " of size: " + fileSize);

                byte[] buffer = new byte[4096]; // 4KB chunks
                int bytesRead;

                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                    dos.flush(); // ensure the chunk is actually sent
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
