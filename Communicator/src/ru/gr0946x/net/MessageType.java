package ru.gr0946x.net;

public enum MessageType {
    MESSAGE,
    INFO,
    REQUEST,
    ERROR;


    public static final String LOGIN    = "LOGIN";
    public static final String REGISTER = "REGISTER";


    public static final String MSG_ALL  = "MSG_ALL";
    public static final String MSG      = "MSG";


    public static final String HISTORY_CMD  = "/history ";
    public static final String SEARCH_CMD   = "/search ";
    public static final String MSG_CMD      = "/msg ";
}