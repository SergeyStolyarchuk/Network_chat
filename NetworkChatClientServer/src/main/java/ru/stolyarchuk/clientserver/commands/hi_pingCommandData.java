package ru.stolyarchuk.clientserver.commands;

import java.io.Serializable;

public class hi_pingCommandData implements Serializable{

    private final String message;


    public hi_pingCommandData(String message)  {
        this.message = message;
    }


    public String getMessage() {
        return message;
    }
}
