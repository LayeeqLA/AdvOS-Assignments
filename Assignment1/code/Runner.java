package code;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;

public class Runner {

    private static int nodeCount = -1; // T1
    private static int nodeId;
    private static int minPerActive; // T2
    private static int maxPerActive; // T3
    private static int minSendDelay; // T4
    private static int snapshotDelay; // T5
    private static int maxNumber; // T6 -> maxNoOfMessagesThatCanBeSent
    private static String configPath;
    private static String outputPath;

    public static void main(String[] args) {
        System.out.println("*****Initializing Service*****");

        if (args.length != 2) {
            System.err.println("Need exactly TWO args!");
            System.exit(-1);
        }

        List<Node> nodes = null;
        try {
            nodeId = Integer.parseInt(args[0]);
            System.out.println("PID for this host: " + nodeId);

            configPath = args[1];

            nodes = processConfig();
            Node currentNode = getNodeById(nodes, nodeId);
            System.out.println("\n****CURRENT NODE****");
            currentNode.printConfig();

            System.out.println("\n*****Starting connectivity activies*****");
            CountDownLatch latch = new CountDownLatch(2);
            boolean initializeAsActive = nodeId == Constants.BASE_NODE;
            AliveProbe nodeStatus = new AliveProbe(maxNumber, initializeAsActive);
            VectorClock localClock = new VectorClock(nodeCount);
            Thread receiverThread = new Thread(new SocketService(currentNode,
                    minPerActive, maxPerActive, minSendDelay, snapshotDelay,
                    maxNumber, latch, nodeStatus, localClock));
            // TODO: remove if above params not required in receiver thread
            receiverThread.start();

            Thread.sleep(10000); // TODO: temp
            List<Node> neighborNodes = currentNode.getNeighbors(nodes);
            for (Node node : neighborNodes) {
                int attempts = 0;
                while (node.getChannel() == null && attempts < Constants.CONNECT_MAX_ATTEMPTS) {
                    try {
                        InetSocketAddress addr = new InetSocketAddress(node.getHost(), node.getPort());
                        Thread.sleep(3000); // connection refused fix
                        SctpChannel sc = SctpChannel.open(addr, 0, 0);
                        node.setChannel(sc);
                        System.out.println("Connected successfully to node " + node.getId());
                    } catch (IOException e) {
                        System.err.println("Connect error for node " + node.getId() + " WILL RETRY");
                        System.err.println(e.getMessage() != null ? e.getMessage() : "null");
                        Thread.sleep(Constants.CONNECT_WAIT);
                        attempts++;
                    }
                }

                if (node.getChannel() == null) {
                    System.err.println("Failed to establish connection with node id " + node.getId());
                    throw new InterruptedException("CONNECTION SETUP FAILED");
                }
            }

            // inform send channels setup
            latch.countDown();
            // wait for all receiver channels to be initialized
            latch.await();
            System.out.println("*****CONNECTIONS READY*****\n");

            int neighborCount = neighborNodes.size();
            while (!nodeStatus.isTerminated()) {
                while (nodeStatus.isAlive()) {
                    int messagesToSend = Constants.getRandomNumber(minPerActive, maxPerActive);
                    for (int i = 0; i < messagesToSend; i++) {

                        // choose random neighbor
                        int destinationIndex = Constants.getRandomNumber(0, neighborCount - 1);
                        Node destinationNode = neighborNodes.get(destinationIndex);

                        Message currentMessage = null;
                        synchronized(localClock) {
                            localClock.incrementAndGet(currentNode.getId());
                            currentMessage = new Message(currentNode.getId(), Message.MessageType.APP,
                                Constants.getRandomBroadcastInt(), localClock);
                            currentMessage.print();
                        }

                        // send to chosen neighbor
                        MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0);
                        destinationNode.getChannel().send(currentMessage.toByteBuffer(), messageInfo);
                        nodeStatus.increment();
                        System.out.println("Message #" + nodeStatus.getMessageCount() + " sent to node "
                                + destinationNode.getId());

                        // minSendDelay
                        Thread.sleep(minSendDelay);
                    }
                    nodeStatus.setPassive();
                }
            }

            // Message finishMessage = new Message(currentNode.getId(), Message.MessageType.FINISH, null);
            // for (Node node : neighborNodes) {
            //     MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0);
            //     node.getChannel().send(finishMessage.toByteBuffer(), messageInfo);
            // }

            for(Node node: neighborNodes) {
                node.getChannel().close();
            }

            receiverThread.join(); // received FINISH from all neighbors
            System.out.println("\n*****END*****");

        } catch (NumberFormatException | IOException | InterruptedException | ClassNotFoundException e) {
            System.err.println("xxxxx---Processing error occured---xxxxx");
            System.err.println(e.getMessage());
            e.printStackTrace();
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
        List<String> allLines = Files.readAllLines(Paths.get(configPath));
        int neighborIndex = 0;

        // FIX BOM encoding for UTF-16 and UTF-8 config files
        String firstLine = allLines.get(0);
        if (firstLine.codePointAt(0) == 0xfeff) {
            allLines.set(0, firstLine.substring(1, firstLine.length()));
        }

        for (String line : allLines) {
            // System.out.println(line);

            // remove inline comments
            line = line.split("#")[0].trim();

            if (!Constants.isConfigLineValid(line)) {
                continue;
            }

            if (nodeCount <= 0) {
                // read global params
                System.out.println("Global params: " + line);
                String globalParamStrings[] = line.split(" ");
                nodeCount = Integer.parseInt(globalParamStrings[0]);
                minPerActive = Integer.parseInt(globalParamStrings[1]);
                maxPerActive = Integer.parseInt(globalParamStrings[2]);
                minSendDelay = Integer.parseInt(globalParamStrings[3]);
                snapshotDelay = Integer.parseInt(globalParamStrings[4]);
                maxNumber = Integer.parseInt(globalParamStrings[5]);
            } else {
                if (nodes.size() < nodeCount) {
                    // read node entry
                    String nodeInfo[] = line.split(" ");
                    nodes.add(new Node(Integer.parseInt(nodeInfo[0]), nodeInfo[1], Integer.parseInt(nodeInfo[2])));
                } else {
                    // read neighbour entry
                    nodes.get(neighborIndex).addNeighbors(line);
                    neighborIndex++;
                }
            }
        }

        System.out.println("***PRINTING NODE CONFIG***");
        for (Node node : nodes) {
            node.printConfig();
        }

        // generate outputPath for this node
        outputPath = configPath;
        if (outputPath.endsWith(".txt")) {
            outputPath = outputPath.substring(0, outputPath.length() - 4);
        }
        outputPath = outputPath + "-" + nodeId + ".out";
        System.out.println("TEMP: output file location: " + outputPath);

        return nodes;
    }
}