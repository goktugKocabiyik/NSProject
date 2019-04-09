import client.Message;
import client.MessageType;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;

public class CSMACA {
    // last status
    private MessageType lastLinkStatus = MessageType.FREE;
    private BlockingQueue<Message> sendingQueue;

    private int cap = 4;

    // my address

    public CSMACA(BlockingQueue<Message> sendingQueue) {
        this.sendingQueue = sendingQueue;
    }

    public void sendMessage(byte[] message) {
        // if packet is to be sent, get a frame

        // (int)(Math.random() * (max - min) + 1) + min

        int timeout = (int) (Math.random() * (Math.pow(2, cap) - 50) + 1) + 50;

        // check if channel is idle
        if (lastLinkStatus == MessageType.BUSY) {
            // if not, wait for some random time

            try {
                if (cap < 11) {
                    cap++;
                }

                Thread.sleep(timeout);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        } else {
            // if idle, send a message
            try {
                ByteBuffer toSend = ByteBuffer.allocate(16);
                toSend.put(message);
                Message msg = new Message(MessageType.DATA, toSend);

                cap = 4;

                sendingQueue.put(msg);

//                try {
//                    Thread.sleep((int) (Math.random() * (30) + 1));
//                } catch (InterruptedException ie) {
//                    ie.printStackTrace();
//                }
            } catch (InterruptedException e){
                System.exit(2);
            }
        }
    }

    public void sendMessage(ByteBuffer message) {

        for (int i = message.position(); i < message.capacity(); i++) {
            message.put((byte) 0);
        }

        System.out.println(Arrays.toString(message.array()));

        sendMessage(message.array());
    }

    public MessageType getLastLinkStatus() {
        return lastLinkStatus;
    }

    public void setLastLinkStatus(MessageType lastLinkStatus) {
        this.lastLinkStatus = lastLinkStatus;
    }
}
