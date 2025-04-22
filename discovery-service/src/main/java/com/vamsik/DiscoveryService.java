package com.vamsik;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class DiscoveryService {
    private static final Logger logger = java.util.logging.Logger.getLogger(DiscoveryService.class.getName());

    public static void main(String[] args) {
        int port = 3969;

        if (args.length == 1) {
            port = Integer.parseInt(args[0]);
        }

        List<Entry> entries = new ArrayList<>();

        try {
            ServerSocket serverSocket = new ServerSocket(port);
            logger.info("Discovery server started on port " + port);

            while (true) {
                Socket sock = serverSocket.accept();
                new Thread(() -> handleClient(sock, entries)).start();
            }
        } catch (EOFException | SocketException e) {
            System.out.println("Server socket closed");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket sock, List<Entry> entries) {
        try {
            DataInputStream dis = new DataInputStream(sock.getInputStream());
            DataOutputStream dos = new DataOutputStream(sock.getOutputStream());

            while (true) {
                String command = dis.readUTF();

                switch (command) {
                    case "REGISTER": {
                        String host = dis.readUTF();
                        int localPort = dis.readInt();
                        int fileCount = dis.readInt();
                        Entry e = new Entry(host, localPort);

                        for (int i = 0; i < fileCount; i++) {
                            String fileName = dis.readUTF();
                            e.addFile(fileName);
                        }

                        entries.add(e);
                        break;
                    }
                    case "QUERY": {
                        String fileName = dis.readUTF();
                        List<String> results = new ArrayList<>();

                        for (Entry entry : entries) {
                            String entryHost = entry.getHost();
                            String requestedHost = sock.getInetAddress().getHostAddress();

                            if (entry.hasFile(fileName) && !entryHost.equals(requestedHost)) {
                                results.add(entry.toString());
                            }
                        }

                        if (results.isEmpty()) {
                            dos.writeUTF("NOTFOUND");
                        } else {
                            dos.writeUTF("FOUND");
                            dos.writeInt(results.size());
                            for (String result : results) {
                                dos.writeUTF(result);
                            }
                        }
                        break;
                    }
                    case "LIST": {
                        dos.writeInt(entries.size());
                        for (Entry entry : entries) {
                            dos.writeUTF(entry.getFile());
                        }
                        break;
                    }
                    default:
                        dos.writeUTF("UNKNOWN");
                        logger.info("Unknown command: " + command);
                        break;
                }

                dos.flush();
            }
        } catch (SocketException | EOFException e) {
            logger.info("Client disconnected: " + sock.getInetAddress().getHostAddress());
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
}
