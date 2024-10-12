package code;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;

import code.Message.MessageType;

public class SocketService implements Runnable {

    private Node selfNode;
    private int minPerActive; // T2
    private int maxPerActive; // T3
    private int minSendDelay; // T4
    private int snapshotDelay; // T5
    private int maxNumber; // T6 -> maxNoOfMessagesThatCanBeSent
    private CountDownLatch latch;
    private LocalState localState;
    private List<Thread> neighborThreads;
    private Map<Integer, Integer> receivedInfo;

    public SocketService(Node selfNode, int minPerActive, int maxPerActive, int minSendDelay,
            int snapshotDelay, int maxNumber, CountDownLatch latch, LocalState state) {
        this.selfNode = selfNode;
        this.minPerActive = minPerActive;
        this.maxPerActive = maxPerActive;
        this.minSendDelay = minSendDelay;
        this.snapshotDelay = snapshotDelay;
        this.maxNumber = maxNumber;
        this.latch = latch;
        this.localState = state;
    }

    @Override
    public void run() {
        neighborThreads = new ArrayList<>();
        receivedInfo = new ConcurrentHashMap<>();

        // SETUP SERVER
        InetSocketAddress addr = new InetSocketAddress(selfNode.getPort());
        SctpServerChannel ssc;
        try {
            ssc = SctpServerChannel.open();
            ssc.bind(addr);
            System.out.println("Started SERVER on nodeId: " + selfNode.getId() + " on port: " + selfNode.getPort());

            for (int i = 0; i < selfNode.getNeighborIds().length; i++) {
                SctpChannel clientConnection = ssc.accept();
                Thread clientThread = new Thread(new ClientHandler(selfNode, clientConnection, receivedInfo,
                        latch, localState));
                clientThread.start();
                neighborThreads.add(clientThread);
            }

            // inform sender thread all receivers are connected
            latch.countDown();

            // wait for send channel
            latch.await();

            for (Thread clientThread : neighborThreads) {
                clientThread.join();
            }

            printReceivedInformation();
            ssc.close();

        } catch (IOException | InterruptedException e) {
            System.err.println("xxxxx---SOCKET SERVICE ERROR---xxxxx");
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private void printReceivedInformation() {
        System.out.println("*****RECEIVED INFORMATION*****");
        for (Entry<Integer, Integer> info : receivedInfo.entrySet()) {
            System.out.println(info.getKey() + " -> " + info.getValue());
        }
    }

    private class ClientHandler implements Runnable {

        private final Node currentNode;
        private final SctpChannel channel;
        private Map<Integer, Integer> receivedData;
        private final CountDownLatch latch;
        private LocalState localState;

        public ClientHandler(Node currentNode, SctpChannel channel, Map<Integer, Integer> receivedData,
                CountDownLatch latch, LocalState localState) {
            this.currentNode = currentNode;
            this.channel = channel;
            this.receivedData = receivedData;
            this.latch = latch;
            this.localState = localState;
        }

        @Override
        public void run() {
            try {
                int pid = -1;

                latch.await(); // wait for send connections to be ready

                while (true) {
                    // keep listening and receive incoming messages
                    ByteBuffer buf = ByteBuffer.allocateDirect(Constants.MAX_MSG_SIZE);
                    channel.receive(buf, null, null);

                    Message message = Message.fromByteBuffer(buf);
                    if (pid == -1) {
                        pid = message.getSender();
                        Thread.currentThread().setName("RECEIVER-" + pid);
                    } else {
                        assert pid == message.getSender();
                        // SHOULD RECEIVE SAME SENDER PID ON A SINGLE THREAD
                    }
                    synchronized (localState) {
                        if (message.getmType() == MessageType.APP) {
                            localState.setActive();
                            localState.addChannelMessage(pid);
                            receivedData.merge(message.getSender(), message.getData(), Integer::sum);
                            VectorClock messageClock = message.getClock();
                            localState.getClock().mergeMessageClockAndIncrement(messageClock, currentNode.getId());
                            localState.getClock().print("After recv: ");
                        }
                        // } else if (message.getmType() == MessageType.FINISH) {
                        // System.out.println("Received FINISH SINGAL from node " + pid);
                        // break;
                        else if (message.getmType() == MessageType.MARKER) {
                            System.out.println("MARKER RECVD FROM " + pid);
                            if (localState.isSnapshotActive()) {
                                // CASE 2: Snapshot processing is active
                                localState.addMarkerReceivedAndGet(pid);
                            } else {
                                // CASE 1: Snapshot processing is not active
                                // This marker message starts the snapshot process
                                // also records this channel as marked
                                localState.setSnapshotActive(currentNode.getId(), pid, currentNode.getNeighborIds());
                                localState.addMarkerReceivedAndGet(pid);
                                new Thread(new SnapshotService(localState, currentNode), "SNAP-SRVC").start(); 
                            }

                            // CHECK IF ALL MARKERS RECEIVED
                            if (localState.getMarkerCount() == currentNode.getNeighborCount()) {
                                // LOCAL SNAPSHOT PROCESS FINISHED; CC DUE;
                                localState.setSnapshotInactive();
                            }

                            // CHECK IF READY FOR CC TO PARENT
                            if (localState.getChildRecordsLength() == currentNode.getChildrenCount()
                                    && !localState.isSnapshotActive()) {
                                sendConvergeCastToParent(selfNode, localState);
                                localState.clearSnapshotData(); // done with snapshot
                            }
                        
                        } else if (message.getmType() == MessageType.CC) {
                            message.print();
                            int childRecordCount = localState.addChildRecordAndGet(pid, message.getStateRecords());
                            if (childRecordCount == currentNode.getChildrenCount()
                                    && !localState.isSnapshotActive()) {
                                sendConvergeCastToParent(selfNode, localState);
                                localState.clearSnapshotData(); // done with snapshot
                            }
                        
                        } else if (message.getmType() == MessageType.FINISH) {
                            assert localState.isMapTerminated() && localState.isSystemTerminated();
                            Message finishMessage = new Message(currentNode.getId(), Message.MessageType.FINISH);
                            for (Node node : currentNode.getChildren()) {
                                MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0);
                                node.getChannel().send(finishMessage.toByteBuffer(), messageInfo);
                            }
                            break;  // stop listening to messages

                        } else {
                            System.out.println(message.getmType() + " unexpected!");
                            break;
                        }
                    }
                }
                System.out.println("FINISHED RECEIVING MESSAGES FOR: " + Thread.currentThread().getName());
            } catch (IOException | ClassNotFoundException | InterruptedException e) {
                System.err.println("xxxxx---CLIENT HANDLER ERROR--> " + Thread.currentThread().getName());
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
        }

    }

    private synchronized void sendConvergeCastToParent(Node selfNode, LocalState localState)
            throws ClassNotFoundException, IOException {
        if(selfNode.getId() == Constants.BASE_NODE) {
            // ROOT NODE
            // TODO: check termination
            // TODO: set system termination
            return;
        }

        // NON ROOT NODES
        List<StateRecord> combinedStateRecords = new ArrayList<>();
        combinedStateRecords.add(localState.getStateRecord());
        localState.getChildRecords().values().stream().forEach(combinedStateRecords::addAll);
        Message messageToParent = new Message(selfNode.getId(), MessageType.CC, combinedStateRecords);
        MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0);
        selfNode.getParent().getChannel().send(messageToParent.toByteBuffer(), messageInfo);
        messageToParent.print(" DESTINATION: " + selfNode.getParent().getId());
    }

}
