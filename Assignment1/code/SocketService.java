package code;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

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
    private AliveProbe nodeStatus;
    private VectorClock localClock;
    private List<Thread> neighborThreads;
    private Map<Integer, Integer> receivedInfo;

    public SocketService(Node selfNode, int minPerActive, int maxPerActive, int minSendDelay,
            int snapshotDelay, int maxNumber, CountDownLatch latch, AliveProbe nodeStatus,
            VectorClock localClock) {
        this.selfNode = selfNode;
        this.minPerActive = minPerActive;
        this.maxPerActive = maxPerActive;
        this.minSendDelay = minSendDelay;
        this.snapshotDelay = snapshotDelay;
        this.maxNumber = maxNumber;
        this.latch = latch;
        this.nodeStatus = nodeStatus;
        this.localClock = localClock;
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

            for (int i = 0; i < selfNode.getNeighbors().length; i++) {
                SctpChannel clientConnection = ssc.accept();
                Thread clientThread = new Thread(new ClientHandler(selfNode, clientConnection, receivedInfo, 
                        latch, nodeStatus, localClock));
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
        private AliveProbe nodeStatus;
        private VectorClock localClock;

        public ClientHandler(Node currentNode, SctpChannel channel, Map<Integer, Integer> receivedData,  
                CountDownLatch latch, AliveProbe nodeStatus, VectorClock localClock) {
            this.currentNode = currentNode;
            this.channel = channel;
            this.receivedData = receivedData;
            this.latch = latch;
            this.nodeStatus = nodeStatus;
            this.localClock = localClock;
        }

        @Override
        public void run() {
            try {
                int recvCount = 0;
                int pid = -1;
                
                latch.await();  // wait for send connections to be ready
                // TODO: while (!nodeStatus.isTerminated()) {
                    
                while(true) {// keep listening and receive incoming messages
                    ByteBuffer buf = ByteBuffer.allocateDirect(Constants.MAX_MSG_SIZE);
                    channel.receive(buf, null, null);
                    recvCount++;
                    nodeStatus.setActive();                    

                    Message message = Message.fromByteBuffer(buf);
                    if (pid == -1) {
                        pid = message.getSender();
                    } else {
                        assert pid == message.getSender();
                        // SHOULD RECEIVE SAME SENDER PID ON A SINGLE THREAD
                    }
                    if (message.getmType() == MessageType.APP) {
                        synchronized(localClock){
                            receivedData.merge(message.getSender(), message.getData(), Integer::sum);
                            VectorClock messageClock = message.getClock();
                            localClock.mergeMessageClockAndIncrement(messageClock, currentNode.getId());
                            localClock.print("After recv: ");
                    }
                    // } else if (message.getmType() == MessageType.FINISH) {
                    //     System.out.println("Received FINISH SINGAL from node " + pid);
                    //     break;
                    } else {
                        System.out.println(message.getmType() + " unexpected!");
                        break;
                    }
                }
                System.out.println("Received " + recvCount + " messages from node " + pid);
            } catch (IOException | ClassNotFoundException | InterruptedException e) {
                System.err.println("xxxxx---CLIENT HANDLER ERROR---xxxxx");
                System.err.println("THREAD: " + Thread.currentThread().getName());
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
        }

    }

}
