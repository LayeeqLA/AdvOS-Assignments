package code;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;

public class Runner {

    static int nodeCount = -1;
    static int nodeId;
    static int nodesConnected = 0;

    public static void main(String[] args) {
        System.out.println("Starting Service!");

        if (args.length != 1) {
            System.err.println("Need exactly one arg");
            System.exit(-1);
        }

        List<Node> nodes = null;
        try {
            nodeId = Integer.parseInt(args[0]);
            System.out.println("PID for this host: " + nodeId);

            nodes = processConfig();
            Node currentNode = getNodeById(nodes, nodeId);
            Thread receiverThread = new Thread(new SocketService(currentNode, nodes.size() - 1));
            receiverThread.start();

            for (Node node : nodes) {
                if (node == currentNode) {
                    continue;
                }
                int attempts = 0;
                while (attempts < Constants.CONNECT_MAX_ATTEMPTS) {
                    try {
                        InetSocketAddress addr = new InetSocketAddress(node.getHost(), node.getPort());
                        SctpChannel sc = SctpChannel.open(addr, 0, 0);
                        node.setChannel(sc);
                    } catch (IOException e) {
                        System.out.println("Connect error for node " + node.getId());
                        Thread.sleep(Constants.CONNECT_WAIT);
                        attempts++;
                    }
                }

                if (node.getChannel() == null) {
                    System.err.println("Failed to establish connection with node id " + node.getId());
                    throw new InterruptedException("CONNECTION SETUP FAILED");
                }
            }

            int count = 0;
            while (count < Constants.MAX_MESSAGES) {
                Thread.sleep(Constants.getRandomWait());
                Message currentMessage = new Message(currentNode.getId(), Message.MessageType.DATA,
                        Constants.getRandomBroadcastInt());
                currentMessage.print();
                for (Node node : nodes) {
                    if (node == currentNode) {
                        continue;
                    }
                    MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0); // MessageInfo for SCTP layer
                    node.getChannel().send(currentMessage.toByteBuffer(), messageInfo);
                }
            }

            if (currentNode.getId() == Constants.BASE_NODE) {
                // received FINISH from ALL
                receiverThread.join();
                MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0); // MessageInfo for SCTP layer
                Message terminateMessage = new Message(currentNode.getId(), Message.MessageType.TERMINATE, null);
                for (Node node : nodes) {
                    if (node == currentNode) {
                        continue;
                    }
                    node.getChannel().send(terminateMessage.toByteBuffer(), messageInfo);
                    Thread.sleep(2000);
                    node.getChannel().close(); // TODO: check for any issues before msg is read by other processes
                }
            } else {
                Node baseNode = getNodeById(nodes, Constants.BASE_NODE);
                MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0); // MessageInfo for SCTP layer
                Message finishMessage = new Message(currentNode.getId(), Message.MessageType.TERMINATE, null);
                baseNode.getChannel().send(finishMessage.toByteBuffer(), messageInfo);
                receiverThread.join(); // received TERMINATE from base node
            }

        } catch (NumberFormatException | IOException | InterruptedException | ClassNotFoundException e) {
            System.err.println("xxxxx---Processing error occured---xxxxx");
            System.err.println(e.getMessage());
            System.err.println(e.getStackTrace());
        } finally {
            if (nodes != null) {
                for (Node node : nodes) {
                    try {
                        if (node.getChannel() != null) {
                            node.getChannel().close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    private static Node getNodeById(List<Node> nodes, int nodeId) throws InterruptedException {
        for (Node node : nodes) {
            if (node.getId() == nodeId) {
                return node;
            }
        }
        throw new InterruptedException("NODE NOT FOUND FROM NODE LIST -> NodeID: " + nodeId);
    }

    public static List<Node> processConfig() throws IOException {
        List<Node> nodes = Collections.synchronizedList(new ArrayList<>());
        List<String> allLines = Files.readAllLines(Paths.get(Constants.CONFIG_PATH));

        // FIX BOM encoding for UTF-16 and UTF-8 config files
        String firstLine = allLines.get(0);
        if (firstLine.codePointAt(0) == 0xfeff) {
            allLines.set(0, firstLine.substring(1, firstLine.length()));
        }

        for (String line : allLines) {
            // System.out.println(line);
            line = line.trim();
            if (line.startsWith("#") || "".equalsIgnoreCase(line)) {
                // ignore this line
                continue;
            }

            if (nodeCount <= 0) {
                // read number of nodes; parse as int
                nodeCount = Integer.parseInt(line.split(" ")[0]);
                System.out.println("TOTAL NODES: " + nodeCount);
            } else {
                // read node entry
                String nodeInfo[] = line.split(" ");
                nodes.add(new Node(Integer.parseInt(nodeInfo[0]), nodeInfo[1], Integer.parseInt(nodeInfo[2])));
            }
        }

        if (nodeCount != nodes.size()) {
            System.err.println("Config file node count mismatch");
            System.exit(-1);
        }

        return nodes;
    }
}