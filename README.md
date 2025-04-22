# Peer 2 Peer File Share

## Description

This is a peer-to-peer file sharing application that allows users to share files with each other over a local network. The application uses Java's socket library and multithreading to handle multiple connections simultaneously. The application is designed to be simple and easy to use, with a graphical user interface (GUI) built using JavaFX.

## Features
- Peer-to-peer file sharing
- Multithreading support for handling multiple connections
- Service discovery using self registration
- Graphical user interface (GUI) built with JavaFX
- File transfer progress indicator

## Requirements
- Java 17 or higher
- JavaFX
- Maven (for building the project)

## Getting Started
- Clone the repository:
```bash
   git clone https://github.com/vmskonakanchi/peer-2-peer-file-share.git
```
- Navigate to the project directory:
```bash
   cd peer-2-peer-file-share
```
- Build the project using Maven:
```bash
   mvn clean install
```
- Run the application:
```bash
   java -jar discovery-service/target/vamsi-discovery-0.1.jar <port
   java -jar ./out/torrent-service.jar <port> <service-discovery-server-host> <service-discovery-server-port> <folder-to-download-or-share>
```

## Usage
1. Start the discovery service on a specific port.
2. Start the torrent service on a specific port, providing the host and port of the discovery service and the folder to download or share files.
3. Use the GUI to select files to download or share.
4. Monitor the file transfer progress in the GUI.
5. Once the file transfer is complete, the file will be available in the specified folder.

## Contributing
If you would like to contribute to this project, please fork the repository and create a pull request with your changes. We welcome contributions from the community!

## License
This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.