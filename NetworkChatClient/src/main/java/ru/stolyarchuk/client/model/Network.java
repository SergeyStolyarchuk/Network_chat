package ru.stolyarchuk.client.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.stolyarchuk.clientserver.Command;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Network {

    public static final int SERVER_PORT = 8189;
    public static final String SERVER_HOST = "localhost";

    private static Network INSTANCE;

    private final int port;
    private final String host;
    private Socket socket;
    private ObjectInputStream socketInput;
    private ObjectOutputStream socketOutput;

    private final List<ReadCommandListener> listeners = new CopyOnWriteArrayList<>();
    private boolean connected;
    private String currentUsername;
    private ExecutorService executorService;
    private final static Logger LOGGER = LogManager.getLogger(Network.class);

    public static Network getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Network();
        }

        return INSTANCE;
    }

    private Network(String host, int port) {
        this.host = host;
        this.port = port;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    private Network() {
        this(SERVER_HOST, SERVER_PORT);
    }

    public boolean connect() {
        try {
            socket = new Socket(host, port);
            socketOutput = new ObjectOutputStream(socket.getOutputStream());
            socketInput = new ObjectInputStream(socket.getInputStream());
            startReadMessageProcess();
            connected = true;
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.info("Не удалось установить соединение");
            return false;
        }
    }

    public void sendAuthMessage(String login, String password) throws IOException {
        sendCommand(Command.authCommand(login, password));
    }

    public void sendHi_pingMessage(String message) throws IOException {
        sendCommand(Command.hi_pingCommand(message));
    }

    public void sendMessage(String message) throws IOException {
        LOGGER.info("send public message");
        sendCommand(Command.publicMessageCommand(message));
    }

    private void sendCommand(Command command) throws IOException {
        try {
            socketOutput.writeObject(command);
        } catch (IOException e) {
            LOGGER.info("Не удалось отправить сообщение на сервер");
            throw e;
        }
    }

    public void sendPrivateMessage(String recipient, String message) throws IOException {
        sendCommand(Command.privateMessageCommand(recipient, message));
    }

    public void startReadMessageProcess() {
        executorService.execute(() -> {
            while (true) {
                try {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    Command command = readCommand();
                    if (command.getType() == null) {
                        continue;
                    }
                    for (ReadCommandListener messageListener : listeners) {
                        messageListener.processReceivedCommand(command);
                    }
                } catch (IOException e) {
                    LOGGER.info("Не удалось прочитать сообщения от сервера");
                    close();
                    break;
                }
            }
        });
    }

    private Command readCommand() throws IOException {
        Command command = null;
        try {
            command = (Command) socketInput.readObject();
        } catch (ClassNotFoundException e) {
            LOGGER.error("Failed to read Command class");
            e.printStackTrace();
        }

        return command;
    }

    public ReadCommandListener addReadMessageListener(ReadCommandListener listener) {
        listeners.add(listener);
        return listener;
    }

    public void removeReadMessageListener(ReadCommandListener listener) {
        listeners.remove(listener);
    }

    public void close() {
        try {
            connected = false;
            executorService.shutdownNow();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setCurrentUsername(String currentUsername) {
        this.currentUsername = currentUsername;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    public boolean isConnected() {
        return connected;
    }


}
