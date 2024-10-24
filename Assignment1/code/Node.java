package code;

import java.io.FileWriter;
import java.io.IOException;
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
    private int[] neighborIds;
    private List<Node> neighbors;
    private Node parent;
    private List<Node> children;
    private int[] childrenIds;
    FileWriter writer;

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

    public int[] getNeighborIds() {
        return neighborIds;
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
        this.writer = null;
    }

    public void printConfig() {
        System.out.println("ID: " + id + " HOST: " + host + " PORT: " + port
                + " Neighbors: " + Arrays.toString(neighborIds));
    }

    public void printConvergeCast() {
        System.out.println("Running: ID: " + id + " Parent: " + (parent == null ? "-" : parent.getId())
                + " Children: " + Arrays.toString(childrenIds));
    }

    public void addNeighborIds(String allNeighbors) {
        neighborIds = Arrays.stream(allNeighbors.split(" ")).mapToInt(Integer::parseInt).toArray();
    }

    public void setNeighbors(List<Node> nodes) {
        this.neighbors = Arrays.stream(neighborIds).mapToObj(nodes::get).collect(Collectors.toList());
    }

    public static Node getNodeById(List<Node> nodes, int nodeId) {
        return nodes.stream().filter(n -> n.getId() == nodeId).findFirst().orElseGet(null);
    }

    public List<Node> getNeighbors() {
        return neighbors;
    }

    public int getNeighborCount() {
        return neighborIds.length;
    }

    public int getChildrenCount() {
        return childrenIds.length;
    }

    public FileWriter getWriter() {
        return writer;
    }

    public void initWriter(String filePath) throws IOException {
        this.writer = new FileWriter(filePath);
    }

    public void writeLocalState(VectorClock clock) throws IOException {
        this.writer.write(VectorClock.getFileString(clock));
        this.writer.flush();
        System.out.println("recorded state to file");
    }

    public void closeFileWriter() {
        if (this.writer != null) {
            try {
                this.writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
