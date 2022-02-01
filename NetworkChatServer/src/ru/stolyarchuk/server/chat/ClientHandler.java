package ru.stolyarchuk.server.chat;

import ru.stolyarchuk.clientserver.Command;
import ru.stolyarchuk.clientserver.CommandType;
import ru.stolyarchuk.clientserver.commands.AuthCommandData;
import ru.stolyarchuk.clientserver.commands.PrivateMessageCommandData;
import ru.stolyarchuk.clientserver.commands.PublicMessageCommandData;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

import static ru.stolyarchuk.clientserver.CommandType.*;

public class ClientHandler {

    private final MyServer server;
    private final Socket clientSocket;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private String userName;


    public ClientHandler(MyServer server, Socket clientSocket) {
        this.server = server;
        this.clientSocket = clientSocket;
    }

    public void handle() throws IOException {
        inputStream = new ObjectInputStream(clientSocket.getInputStream());
        outputStream = new ObjectOutputStream(clientSocket.getOutputStream());

        new Thread(() -> {
            Command preCommand = null;
            try {
                preCommand = readCommand();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (preCommand.getType() == HI_PING) {

                TimerTask timerTask1 = new TimerTask() {
                    @Override
                    public void run() {

                        try {
                            if (userName == null) {
                                sendCommand(Command.errorCommand("Время на соединение истекло"));
                                clientSocket.close();

                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                };
                Timer timer = new Timer(true);

                timer.schedule(timerTask1, 10000);
            }

            try {
                authenticate();
                readMessages();
            } catch (IOException e) {
                System.err.println("Failed to process message from client");
                e.printStackTrace();
            } finally {
                try {
                    closeConnection();
                } catch (IOException e) {
                    System.err.println("Failed to close connection");
                }
            }
        }).start();

    }

    private void authenticate() throws IOException {

        while (true) {
            Command command = readCommand();
            if (command == null) {
                continue;
            }

            if (command.getType() == AUTH) {

                AuthCommandData data = (AuthCommandData) command.getData();
                String login = data.getLogin();
                String password = data.getPassword();
                String userName = server.getAuthService().getUserNameByLoginAndPassword(login, password);
                if (userName == null) {
                    sendCommand(Command.errorCommand("Некорректные логин и пароль"));
                } else if (server.isUsernameBusy(userName)) {
                    sendCommand(Command.errorCommand("Такой пользователь уже существует!"));
                } else {
                    this.userName = userName;
                    sendCommand(Command.authOkCommand(userName));

                    server.subscribe(this);
                    return;
                }
            }
        }
    }

    private Command readCommand() throws IOException {
        Command command = null;
        try {
            command = (Command) inputStream.readObject();
        } catch (ClassNotFoundException e) {
            System.err.println("Failed to read Command class");
//            e.printStackTrace();
        }

        return command;
    }

    private void closeConnection() throws IOException {
        server.unsubscribe(this);
        clientSocket.close();
    }

    private void readMessages() throws IOException {
        while (true) {
            Command command = readCommand();
            if (command == null) {
                continue;
            }

            switch (command.getType()) {
                case END:
                    return;
                case PRIVATE_MESSAGE: {
                    PrivateMessageCommandData data = (PrivateMessageCommandData) command.getData();
                    String recipient = data.getReceiver();
                    String privateMessage = data.getMessage();
                    server.sendPrivateMessage(this, recipient, privateMessage);
                    break;
                }
                case PUBLIC_MESSAGE: {
                    PublicMessageCommandData data = (PublicMessageCommandData) command.getData();
                    processMessage(data.getMessage());
                }
            }
        }
    }

    private void processMessage(String message) throws IOException {
        this.server.broadcastMessage(message, this);
    }

    public void sendCommand(Command command) throws IOException {
        outputStream.writeObject(command);
    }

    public String getUserName() {
        return userName;
    }
}
