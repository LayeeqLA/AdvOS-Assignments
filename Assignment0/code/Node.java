package code;

import com.sun.nio.sctp.SctpChannel;

public class Node {

    private int id;
    private String host;
    private int port;
    private SctpChannel channel;

    public SctpChannel getChannel() {
        return channel;
    }

    public void setChannel(SctpChannel channel) {
        this.channel = channel;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Node(int id, String host, int port) {
        this.id = id;
        this.host = host;
        this.port = port;
    }

}
