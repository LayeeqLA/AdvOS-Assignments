package code;

import java.util.Arrays;

import com.sun.nio.sctp.SctpChannel;

public class Node {

    private int id;
    private String host;
    private int port;
    private SctpChannel channel;
    private int[] neighbors;

    public int[] getNeighbors() {
        return neighbors;
    }

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

    public void printConfig() {
        System.out.println("ID: " + id + " HOST: " + host + " PORT: " + port
                + " Neighbors: " + Arrays.toString(neighbors));
    }

    public void addNeighbors(String allNeighbors) {
        neighbors = Arrays.stream(allNeighbors.split(" ")).mapToInt(Integer::parseInt).toArray();
    }

}
