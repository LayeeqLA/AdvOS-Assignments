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
    private CountDownLatch latch;
    private LocalState localState;
    private List<Thread> neighborThreads;
    private Map<Integer, Integer> receivedInfo;

    public SocketService(Node selfNode, CountDownLatch latch, LocalState state) {
        this.selfNode = selfNode;
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

            // printReceivedInformation();
            ssc.close();

        } catch (IOException | InterruptedException e) {
            System.err.println("xxxxx---SOCKET SERVICE ERROR---xxxxx");
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    // private void printReceivedInformation() {
    //     System.out.println("*****RECEIVED INFORMATION*****");
    //     for (Entry<Integer, Integer> info : receivedInfo.entrySet()) {
    //         System.out.println(info.getKey() + " -> " + info.getValue());
    //     }
    // }

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

                boolean receiving = true;
                while (receiving) {
                    // keep listening and receive incoming messages
                    ByteBuffer buf = ByteBuffer.allocateDirect(Constants.MAX_MSG_SIZE);
                    channel.receive(buf, null, null);

                    Message message = Message.fromByteBuffer(buf);
                    if (message == null) {
                        // neighbor is closing this connection
                        break;
                    }
                    if (pid == -1) {
                        // first message from neighbor
                        pid = message.getSender();
                        Thread.currentThread().setName("RECEIVER-" + pid);
                    } else {
                        assert pid == message.getSender();
                        // SHOULD RECEIVE SAME SENDER PID ON A SINGLE THREAD
                    }
                    synchronized (localState) {
                        switch (message.getmType()) {
                            case APP:
                                VectorClock messageClock = message.getClock();
                                localState.getClock().mergeMessageClockAndIncrement(messageClock,
                                        currentNode.getId());
                                localState.getClock().print("After recv: ");
                                localState.addChannelAppMessage(pid);
                                receivedData.merge(message.getSender(), message.getData(), Integer::sum);
                                localState.setSystemActive();

                                break;

                            case MARKER:
                                System.out.println("MARKER RECVD FROM " + pid);
                                if (localState.isSnapshotActive()) {
                                    // CASE 1: Snapshot processing is active
                                    localState.addMarkerReceived(pid);
                                } else {
                                    // CASE 2: Snapshot processing is not active
                                    // This marker message starts the local snapshot process
                                    // also records this channel as marked
                                    localState.setSnapshotActive(currentNode.getId(), pid,
                                            currentNode.getNeighborIds());
                                    currentNode.writeLocalState(localState.getClock());
                                    localState.addMarkerReceived(pid);

                                    // send marker message to all neighbors
                                    Message markerMessage = new Message(currentNode.getId(),
                                            Message.MessageType.MARKER);
                                    for (Node neighbor : currentNode.getNeighbors()) {
                                        MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0);
                                        neighbor.getChannel().send(markerMessage.toByteBuffer(), messageInfo);
                                        System.out.println("Marker Message sent to pid " + neighbor.getId());
                                    }
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

                                break;

                            case CC:
                                message.print();
                                int childRecordCount = localState.addChildRecordAndGet(pid,
                                        message.getStateRecords());
                                if (childRecordCount == currentNode.getChildrenCount()
                                        && !localState.isSnapshotActive()) {
                                    sendConvergeCastToParent(selfNode, localState);
                                    localState.clearSnapshotData(); // done with snapshot
                                }

                                break;

                            case FINISH:
                                assert localState.isMapTerminated();
                                message.print(" ======> received");
                                localState.terminateSystem();
                                Message finishMessage = new Message(currentNode.getId(),
                                        Message.MessageType.FINISH);
                                for (Node destNode : currentNode.getChildren()) {
                                    finishMessage.print(" destination: " + destNode.getId());
                                    MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0);
                                    destNode.getChannel().send(finishMessage.toByteBuffer(), messageInfo);
                                }
                                System.out.println("\n---Sent FINISH to child node if any---");
                                receiving = false;
                                currentNode.closeFileWriter();
                                break;

                            default:
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
            } finally {
                try {
                    channel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private synchronized void sendConvergeCastToParent(Node currentNode, LocalState localState)
            throws ClassNotFoundException, IOException {
        if (currentNode.getParent() == null) {
            // ROOT NODE
            List<StateRecord> combinedStateRecords = new ArrayList<>();
            combinedStateRecords.add(localState.getStateRecord());
            localState.getChildRecords().values().stream().forEach(combinedStateRecords::addAll);
            if (combinedStateRecords.stream()
                    .filter(state -> state.isNodeMapActive() || !state.areAllChannelsEmpty())
                    .count() == 0) {
                // terminate the system
                System.out.println("IDENTIFIED DISTRIBUTED SYSTEM TERMINATED");
                localState.terminateSystem();
                currentNode.closeFileWriter();
            } else {
                // wait and start snapshot again at root
                new Thread(new SnapshotStarter(localState, currentNode), "SNAP-SRVC").start();
            }
            return;
        }

        // NON ROOT NODES
        List<StateRecord> combinedStateRecords = new ArrayList<>();
        combinedStateRecords.add(localState.getStateRecord());
        localState.getChildRecords().values().stream().forEach(combinedStateRecords::addAll);
        Message messageToParent = new Message(currentNode.getId(), MessageType.CC, combinedStateRecords);
        MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0);
        currentNode.getParent().getChannel().send(messageToParent.toByteBuffer(), messageInfo);
        messageToParent.print(" Destination: " + currentNode.getParent().getId());
    }

}
