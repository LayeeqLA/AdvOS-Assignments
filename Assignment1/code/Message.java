package code;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

public class Message implements Serializable {
    private Integer sender;
    private MessageType mType;
    private Integer data;
    private VectorClock clock;

    public Message(Integer sender, MessageType mType, Integer data, VectorClock clock) {
        this.sender = sender;
        this.mType = mType;
        this.data = data;
        this.clock = clock;
    }

    public enum MessageType {
        APP,
        MARKER,
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
        System.out.println("Sender: " + sender + " MsgType: " + mType 
                + " Data: " + (data != null ? data : "null")
                + " Clock: " + (clock != null ? clock.toString() : "null"));
    }

}