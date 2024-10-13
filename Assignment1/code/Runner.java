package code;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;

public class Runner {
    ;
    private static int nodeCount = -1; // T1
    private static int minPerActive; // T2
    private static int maxPerActive; // T3
    private static int minSendDelay; // T4
    private static int snapshotDelay; // T5
    private static int maxNumber; // T6 -> maxNoOfMessagesThatCanBeSent
    private static String outputPath;

    public static void main(String[] args) {
        System.out.println("*****Initializing Service*****");

        if (args.length != 2) {
            System.err.println("Need exactly TWO args!");
            System.exit(-1);
        }

        Node currentNode = null;
        try {
            int nodeId = Integer.parseInt(args[1]);
            String configPath = args[0];
            System.out.println("PID for this host: " + nodeId);

            currentNode = processConfig(configPath, nodeId);
            System.out.println("\n****CURRENT NODE****");
            currentNode.printConfig();

            System.out.println("\n*****Starting connectivity activies*****");
            CountDownLatch latch = new CountDownLatch(2);
            boolean initializeAsActive = nodeId == Constants.BASE_NODE;
            LocalState localState = new LocalState(maxNumber, initializeAsActive, nodeCount);
            Thread receiverThread = new Thread(new SocketService(currentNode,
                    latch, localState), "RECV-SRVC");
            receiverThread.start();

            for (Node node : currentNode.getNeighbors()) {
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

            if (currentNode.getParent() == null) {
                Thread.sleep(10000);
                SnapshotStarter.snapshotDelay = snapshotDelay;
                new Thread(new SnapshotStarter(localState, currentNode), "SNAP-SRVC").start();
            }

            while (!localState.isMapTerminated()) {
                if (localState.isSystemActive()) {
                    int messagesToSend = Constants.getRandomNumber(minPerActive, maxPerActive);
                    for (int i = 0; i < messagesToSend; i++) {

                        // choose random neighbor
                        int destinationIndex = Constants.getRandomNumber(0, currentNode.getNeighborCount() - 1);
                        Node destinationNode = currentNode.getNeighbors().get(destinationIndex);

                        Message currentMessage = null;
                        synchronized (localState) {
                            localState.getClock().incrementAndGet(currentNode.getId());
                            currentMessage = new Message(currentNode.getId(), Message.MessageType.APP,
                                    Constants.getRandomBroadcastInt(), localState.getClock());

                            // send to chosen neighbor
                            MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0);
                            destinationNode.getChannel().send(currentMessage.toByteBuffer(), messageInfo);
                            localState.incrementMessageCount();
                            currentMessage.print(" Destination: " + destinationIndex
                                    + " MC: " + localState.getMessageCount());
                        }

                        // minSendDelay
                        Thread.sleep(minSendDelay);
                    }
                    localState.setSystemPassive();
                }
            }

            // WAIT FOR SNAPSHOT TO INFORM TERMINATION OF SYSTEM
            localState.getTerminationLatch().await();

            if (currentNode.getParent() == null) {
                // ROOT sends FINISH to terminate the system
                Message finishMessage = new Message(currentNode.getId(), Message.MessageType.FINISH);
                for (Node node : currentNode.getChildren()) {
                    MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0);
                    node.getChannel().send(finishMessage.toByteBuffer(), messageInfo);
                }
                System.out.println("\n---Sent FINISH to child node if any---");

            } else {
                Set<Integer> childrenIdList = Arrays.stream(currentNode.getChildrenIds()).boxed()
                        .collect(Collectors.toSet());
                for (Node node : currentNode.getNeighbors()) {
                    if (!childrenIdList.contains(node.getId())
                            && node != currentNode.getParent()) {
                        System.out.println("closing connection " + currentNode.getId() + " --> " + node.getId());
                        node.getChannel().close();
                    }
                }
            }

            receiverThread.join(); // received FINISH from all neighbors

            System.out.println("\n*****END*****\n\n");

            if (currentNode.getParent() == null) {
                ConsistencyChecker.checkGlobalStateConsistency(nodeCount, configPath);
            }

        } catch (NumberFormatException | IOException | InterruptedException | ClassNotFoundException e) {
            System.err.println("xxxxx---Processing error occured---xxxxx");
            System.err.println(e.getMessage());
            e.printStackTrace();
        } finally {
            if (currentNode != null && currentNode.getNeighbors() != null) {
                for (Node node : currentNode.getNeighbors()) {
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

    public static Node processConfig(String configPath, int currentNodeId) throws IOException {
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
                    nodes.get(neighborIndex).addNeighborIds(line);
                    neighborIndex++;
                }
            }
        }

        constructConvergeCastTree(nodes, currentNodeId);

        System.out.println("***PRINTING NODE CONFIG***");
        for (Node node : nodes) {
            node.printConfig();
        }

        Node currentNode = Node.getNodeById(nodes, currentNodeId);
        currentNode.printConvergeCast();

        // generate outputPath for this node
        outputPath = configPath;
        if (outputPath.endsWith(".txt")) {
            outputPath = outputPath.substring(0, outputPath.length() - 4);
        }
        outputPath = outputPath + "-" + currentNodeId + ".out";
        System.out.println("Output file location: " + outputPath);
        currentNode.initWriter(outputPath);

        currentNode.setNeighbors(nodes);
        return currentNode;
    }

    public static void constructConvergeCastTree(List<Node> nodes, int runnerNodeId) {
        assert nodeCount != -1;
        // Node runningNode = nodes.get(nodeId);
        boolean[] visited = new boolean[nodeCount]; // false by default
        int[] parentId = new int[nodeCount];
        Arrays.fill(parentId, -1);
        Queue<Integer> queue = new LinkedList<>();
        queue.add(Constants.BASE_NODE);
        visited[Constants.BASE_NODE] = true;

        while (!queue.isEmpty()) {
            int currentNodeId = queue.remove();
            Node currentNode = nodes.get(currentNodeId);
            for (int neighborId : currentNode.getNeighborIds()) {
                if (!visited[neighborId]) {
                    queue.add(neighborId);
                    parentId[neighborId] = currentNodeId;
                    visited[neighborId] = true;
                }
            }
        }

        // System.out.println("*** PARENT IDs ***");
        // System.out.println(Arrays.toString(parentId));
        // System.out.println("*** VISITED ***");
        // System.out.println(Arrays.toString(visited));

        // add array information to Node structures
        Node currentNode = nodes.get(runnerNodeId);
        if (runnerNodeId != Constants.BASE_NODE) {
            // root node will have parent == null
            currentNode.setParent(nodes.get(parentId[runnerNodeId]));
        }

        List<Node> childrenNodes = nodes.stream().filter(node -> parentId[node.getId()] == runnerNodeId)
                .collect(Collectors.toList());
        currentNode.setChildren(childrenNodes);
    }
}