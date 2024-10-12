package code;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Message implements Serializable {
    private Integer sender;
    private MessageType mType;
    private Integer data;
    private VectorClock clock;
    private List<StateRecord> stateRecords;

    public Message(Integer sender, MessageType mType, Integer data, VectorClock clock) {
        // For APP
        this.sender = sender;
        this.mType = mType;
        this.data = data;
        this.clock = clock;
    }

    public Message(Integer sender, MessageType mType) {
        // For Marker or Convergecast
        this.sender = sender;
        this.mType = mType;
    }

    public Message(Integer sender, MessageType mType, List<StateRecord> records) {
        // For Marker or Convergecast
        this.sender = sender;
        this.mType = mType;
        this.stateRecords = records;
    }

    public enum MessageType {
        APP,
        MARKER,
        CC,
        FINISH,
        TERMINATE,
        INVALID,
        ;

    }

    public Integer getSender() {
        return sender;
    }

    public void setSender(int sender) {
        this.sender = sender;
    }

    public MessageType getmType() {
        return mType;
    }

    public void setmType(MessageType mType) {
        this.mType = mType;
    }

    public Integer getData() {
        return data;
    }

    public void setData(Integer data) {
        this.data = data;
    }

    public VectorClock getClock() {
        return clock;
    }

    public List<StateRecord> getStateRecords() {
        return stateRecords;
    }

    // Convert current instance of Message to ByteBuffer
    // in order to send message over SCTP
    public ByteBuffer toByteBuffer() throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(this);
        oos.flush();

        ByteBuffer buf = ByteBuffer.allocateDirect(bos.size());
        buf.put(bos.toByteArray());

        oos.close();
        bos.close();

        // Buffer needs to be flipped after writing
        // Buffer flip should happen only once
        buf.flip();
        return buf;
    }

    // Retrieve Message from ByteBuffer received from SCTP
    public static Message fromByteBuffer(ByteBuffer buf) throws IOException, ClassNotFoundException {
        // Buffer needs to be flipped before reading
        // Buffer flip should happen only once
        buf.flip();
        byte[] data = new byte[buf.limit()];
        buf.get(data);
        buf.clear();

        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bis);
        Message msg = (Message) ois.readObject();

        bis.close();
        ois.close();

        return msg;
    }

    public void print() {
        switch (mType) {
            case APP:
                System.out.println("Sender: " + sender + " MsgType: " + mType
                        + " Data: " + data + " Clock: " + clock.toString());
                break;
            case MARKER:
                System.out.println("Sender: " + sender + " MsgType: " + mType);
                break;
            case CC:
                System.out.println("Sender: " + sender + " MsgType: " + mType + " StateRecords: "
                        + stateRecords.stream().map(StateRecord::toString).collect(Collectors.joining()));
                break;
            default:
                break;
        }
    }

    public void print(String postfix) {
        switch (mType) {
            case APP:
                System.out.println("Sender: " + sender + " MsgType: " + mType
                        + " Data: " + data + " Clock: " + clock.toString() + postfix);
                break;
            case MARKER:
                System.out.println("Sender: " + sender + " MsgType: " + mType + postfix);
                break;
            case CC:
                System.out.println("Sender: " + sender + " MsgType: " + mType
                        + " StateRecords: "
                        + stateRecords.stream().map(StateRecord::toString).collect(Collectors.joining()) + postfix);
                break;
        }
    }

}