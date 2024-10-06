package code;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.sun.nio.sctp.SctpChannel;

public class Node {

    private int id;
    private String host;
    private int port;
    private SctpChannel channel;
    private int[] neighbors;
    private Node parent;
    private List<Node> children;
    private int[] childrenIds;

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

    public SctpChannel getChannel() {
        return channel;
    }

    public void setChannel(SctpChannel channel) {
        this.channel = channel;
    }

    public int[] getNeighbors() {
        return neighbors;
    }

    public Node getParent() {
        return parent;
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    public List<Node> getChildren() {
        return children;
    }

    public void setChildren(List<Node> children) {
        this.children = children;
        this.childrenIds = children.stream().mapToInt(Node::getId).toArray();
    }

    public int[] getChildrenIds() {
        return childrenIds;
    }

    public Node(int id, String host, int port) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.parent = null;
        this.children = new LinkedList<>();
    }

    public void printConfig() {
        System.out.println("ID: " + id + " HOST: " + host + " PORT: " + port
                + " Neighbors: " + Arrays.toString(neighbors));
    }

    public void printConvergeCast() {
        System.out.println("Running: ID: " + id + " Parent: " + (parent == null ? "-" : parent.getId())
                + " Children: " + Arrays.toString(childrenIds));
    }

    public void addNeighbors(String allNeighbors) {
        neighbors = Arrays.stream(allNeighbors.split(" ")).mapToInt(Integer::parseInt).toArray();
    }

    public List<Node> getNeighbors(List<Node> nodes) {
        return Arrays.stream(neighbors).mapToObj(nodes::get).collect(Collectors.toList());
    }

}
