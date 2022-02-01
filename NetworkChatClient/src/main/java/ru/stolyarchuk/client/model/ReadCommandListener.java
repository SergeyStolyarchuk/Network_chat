package ru.stolyarchuk.client.model;

import ru.stolyarchuk.clientserver.Command;

public interface ReadCommandListener {

    void processReceivedCommand(Command command);

}