package entities;

import java.io.Serializable;

/**
 * The universal envelope for all client-server communication in GoNature.
 * Every request and response is wrapped in a {@code Message} and sent over
 * the OCSF TCP socket via Java object serialization.
 */
public class Message implements Serializable {
    
    private static final long serialVersionUID = 1L;

    private String command; // e.g., "GET_ORDERS", "UPDATE_ORDER", "SUCCESS"
    private Object data;    // This can hold a single Order, an ArrayList of Orders, or be null

    public Message(String command, Object data) {
        this.command = command;
        this.data = data;
    }

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }

    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
}