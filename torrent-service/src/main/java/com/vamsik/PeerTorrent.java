package com.vamsik;

import com.vamsik.core.ClientHandler;
import com.vamsik.core.Peer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.concurrent.Task;
import javafx.util.Callback;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class PeerTorrent extends Application {
    private final static Logger logger = Logger.getLogger(PeerTorrent.class.getName());
    private static Peer currentPeer;
    private ListView<String> availableFilesList;
    private ClientHandler clientHandler;
    private ExecutorService executorService;
    private ProgressIndicator progressIndicator;

    public static void main(String[] args) {
        int port = 8000;
        String discoverHost = "localhost";
        int discoverPort = 3969;
        String dirName = ".shared";

        // args[0] = port
        // args[1] = discoveryHost
        // args[2] = discoveryPort
        // args[3] = dirName

        if (args.length > 0) {
            port = Integer.parseInt(args[0]); // port
        }
        if (args.length > 1) {
            discoverHost = args[1]; // discovery host
        }
        if (args.length > 2) {
            discoverPort = Integer.parseInt(args[2]); // discovery port
        }
        if (args.length > 3) {
            dirName = args[3]; // directory name
        }

        logger.info("Starting PeerTorrent with port: " + port + ", discoveryHost: " + discoverHost + ", discoveryPort: " + discoverPort + ", dirName: " + dirName);

        Peer newPeer = new Peer(discoverHost, discoverPort, port, dirName); // creating a new peer instance
        newPeer.start(); // starting the peer thread
        currentPeer = newPeer; // setting the current peer
        launch(args); // launching JavaFX(GUI) application
    }

    @Override
    public void start(Stage stage) throws Exception {
        executorService = Executors.newFixedThreadPool(10);

        Parameters params = getParameters();

        String discoveryHost = "localhost";
        int discoverPort = 3969;
        String dirName = ".shared";

        List<String> rawParams = params.getRaw();

        if (rawParams.size() >= 2) {
            discoveryHost = rawParams.get(1);
        }
        if (rawParams.size() >= 3) {
            discoverPort = Integer.parseInt(rawParams.get(2));
        }
        if (rawParams.size() >= 4) {
            dirName = rawParams.get(3);
        }

        clientHandler = new ClientHandler(discoveryHost, discoverPort, dirName); // Initialize ClientHandler here

        stage.setTitle("Peer Torrent Client");
        progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false); // Initially hide the progress indicator

        // handling when the app is closed
        stage.setOnCloseRequest(event -> {
            logger.info("Closing application...");
            if (currentPeer != null) {
                currentPeer.disconnect();
                executorService.shutdown();
            }
        });

        Label availableFilesLabel = new Label("Available Files:");

        // set up the ListView with custom cell factory for download buttons
        availableFilesList = new ListView<>();
        availableFilesList.setCellFactory(createCellFactory());

        Button refreshFilesButton = getRefreshButton();

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));
        layout.getChildren().addAll(progressIndicator, availableFilesLabel, refreshFilesButton, availableFilesList);

        Scene scene = new Scene(layout, 600, 400);
        stage.setScene(scene);
        stage.show();
    }

    private Callback<ListView<String>, ListCell<String>> createCellFactory() {
        return new Callback<>() {
            @Override
            public ListCell<String> call(ListView<String> param) {
                return new ListCell<>() {
                    private final Button downloadButton = new Button("Download");
                    private final HBox hbox = new HBox();
                    private final Label label = new Label();

                    {
                        hbox.setAlignment(Pos.CENTER_LEFT);
                        hbox.setSpacing(10);

                        // Set the label to grow and fill available space
                        HBox.setHgrow(label, Priority.ALWAYS);

                        downloadButton.setOnAction(event -> {
                            String fileName = getItem();
                            if (fileName != null && !fileName.isEmpty()) {
                                downloadFile(fileName);
                            }
                        });

                        hbox.getChildren().addAll(label, downloadButton);
                    }

                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);

                        if (empty || item == null) {
                            setGraphic(null);
                        } else {
                            label.setText(item);
                            setGraphic(hbox);
                        }
                    }
                };
            }
        };
    }

    private void downloadFile(String fileName) {
        Task<String> checkFileExistTask = clientHandler.checkFileExist(fileName);
        Task<Boolean> downloadFileTask = clientHandler.downloadFile(fileName);

        // Make visible and unbind before setting value
        progressIndicator.setVisible(true);
        progressIndicator.progressProperty().unbind();
        progressIndicator.setProgress(0);

        // Now bind to task
        progressIndicator.progressProperty().bind(checkFileExistTask.progressProperty());

        checkFileExistTask.setOnSucceeded(e -> {
            progressIndicator.progressProperty().unbind(); // Unbind from first task
            String result = checkFileExistTask.getValue();
            if (result != null && !result.isEmpty()) {
                logger.info("File exists on peer: " + result);
                progressIndicator.setProgress(0); // Reset progress
                progressIndicator.progressProperty().bind(downloadFileTask.progressProperty()); // Bind to second task
                executorService.execute(downloadFileTask);
            } else {
                logger.info("File not found on any peer.");
                progressIndicator.setVisible(false);
            }
        });

        checkFileExistTask.setOnFailed(e -> {
            progressIndicator.progressProperty().unbind();
            progressIndicator.setVisible(false);
            logger.info("Failed to check if file exists: " + e.getSource().getException());
        });

        downloadFileTask.setOnSucceeded(e -> {
            progressIndicator.progressProperty().unbind();
            Boolean result = downloadFileTask.getValue();
            if (result) {
                logger.info("File downloaded successfully.");
            } else {
                logger.info("Failed to download file.");
            }
            progressIndicator.setVisible(false);
        });

        downloadFileTask.setOnFailed(e -> {
            progressIndicator.progressProperty().unbind();
            progressIndicator.setVisible(false);
            logger.info("Failed to download file: " + e.getSource().getException());
        });

        executorService.execute(checkFileExistTask); // Execute the task in a separate thread
    }

    private Button getRefreshButton() {
        Button refreshFilesButton = new Button("Refresh Files");

        refreshFilesButton.setOnAction(event -> {
            logger.info("Fetching files...");
            Task<List<String>> task = clientHandler.getAllFiles();

            // Make visible and unbind before setting value
            progressIndicator.setVisible(true);
            progressIndicator.progressProperty().unbind();
            progressIndicator.setProgress(0);

            // Now bind to task
            progressIndicator.progressProperty().bind(task.progressProperty());

            task.setOnSucceeded(e -> {
                progressIndicator.progressProperty().unbind();
                List<String> files = task.getValue();

                if (files.isEmpty()) {
                    logger.info("No files available.");
                } else {
                    availableFilesList.getItems().clear();
                    availableFilesList.getItems().addAll(task.getValue());
                    logger.info("Files fetched successfully.");
                }

                progressIndicator.setVisible(false);
            });

            task.setOnFailed(e -> {
                progressIndicator.progressProperty().unbind();
                logger.info("Failed to fetch files: " + e.getSource().getException());
                progressIndicator.setVisible(false);
            });

            executorService.execute(task); // Execute the task in a separate thread
        });
        return refreshFilesButton;
    }
}