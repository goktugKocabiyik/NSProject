import client.Client;
import client.Message;
import client.MessageType;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MyProtocol{

    // The host to connect to. Set this to localhost when using the audio interface tool.
    private static String SERVER_IP = "netsys2.ewi.utwente.nl"; //"127.0.0.1";
    // The port to connect to. 8954 for the simulation server.
    private static int SERVER_PORT = 8954;
    // The frequency to use.
    private static int frequency = 1200;

    private BlockingQueue<Message> receivedQueue;
    private BlockingQueue<Message> sendingQueue;

    private AddressLayer al;
    private CSMACA csmaca;
    private TCP tcp;

    private int ID;

    public MyProtocol(String server_ip, int server_port, int frequency) {
        receivedQueue = new LinkedBlockingQueue<Message>();
        sendingQueue = new LinkedBlockingQueue<Message>();

        Scanner in = new Scanner(System.in);

        while (true) {
            System.out.println("Enter your ID: ");
            if (in.hasNextInt()) {
                ID = in.nextInt();
                break;
            }
        }

        new Client(SERVER_IP, SERVER_PORT, frequency, receivedQueue, sendingQueue); // Give the client the Queues to use

        csmaca = new CSMACA(sendingQueue);
        tcp = new TCP();

        // creates address layer
        al = new AddressLayer(csmaca, ID);

        new receiveThread(receivedQueue).start(); // Start thread to handle received messages!

        // run application
        try{
            ByteBuffer temp = ByteBuffer.allocate(1024);
            int read = 0;
            while(true){
                read = System.in.read(temp.array()); // Get data from stdin, hit enter to send!
                if(read > 0){
                    ByteBuffer toSend = ByteBuffer.allocate(read-2); // jave includes newlines in System.in.read, so -2 to ignore this
                    toSend.put( temp.array(), 0, read-2 ); // jave includes newlines in System.in.read, so -2 to ignore this
                    Message msg;
                    if( (read-2) > 2 ){
                        msg = new Message(MessageType.DATA, toSend);
                    } else {
                        msg = new Message(MessageType.DATA_SHORT, toSend);
                    }
                    sendingQueue.put(msg);
                }
            }
        } catch (InterruptedException e){
            System.exit(2);
        } catch (IOException e){
            System.exit(2);
        }
    }

    private class receiveThread extends Thread {
        private BlockingQueue<Message> receivedQueue;

        public receiveThread(BlockingQueue<Message> receivedQueue){
            super();
            this.receivedQueue = receivedQueue;
        }

        public void printByteBuffer(ByteBuffer bytes, int bytesLength){
            for(int i=0; i<bytesLength; i++){
                System.out.print( Byte.toString( bytes.get(i) )+" " );
            }
            System.out.println();
        }

        public void run(){
            while(true) {
                try{
                    Message m = receivedQueue.take();
                    if (m.getType() == MessageType.BUSY){
                        System.out.println("BUSY");
                        csmaca.setLastLinkStatus(m.getType());
                    } else if (m.getType() == MessageType.FREE){
                        System.out.println("FREE");
                        csmaca.setLastLinkStatus(m.getType());
                    } else if (m.getType() == MessageType.DATA){
                        System.out.println("DATA: ");

                        synchronized (al) {
                            // if message is a link state update
                            if (m.getData().get(0) >> 4 == 0x01) {
                                al.updateTable(m.getData().array());
                            }
                            // if message is an IP message and we are the next hop
                            else if ((m.getData().get(0) >> 4 == 0x02) && ((m.getData().get(1) & 0x0f) == ID)) {

                                // if we are the destination, send to for further processing to TCP
                                if ((m.getData().get(1) >> 4) == ID) {
                                    // send the message and the source to TCP
                                    tcp.processPacket(Arrays.copyOfRange(m.getData().array(), 2, m.getData().position()), (m.getData().get(1) & 0x0f));
                                } else {
                                    // if we are an intermediate node, forward it further
                                    int nextHop = al.getNextHop(m.getData().get(1) >> 4);

                                    if (nextHop == -1) {
                                        // no entry in the table - drop the packet?

                                    } else {
                                        // replace the next hop
                                        m.getData().put(1, (byte) (((m.getData().get(1) >> 4) << 4) & nextHop));
                                    }
                                }
                            }
                        }

                        printByteBuffer( m.getData(), m.getData().capacity() ); //Just print the data
                    } else if (m.getType() == MessageType.DATA_SHORT){
                        System.out.println("DATA_SHORT: ");
                        printByteBuffer( m.getData(), m.getData().capacity() ); //Just print the data
                    } else if (m.getType() == MessageType.DONE_SENDING){
                        System.out.println("DONE_SENDING");
                    } else if (m.getType() == MessageType.HELLO){
                        System.out.println("HELLO");
                    } else if (m.getType() == MessageType.SENDING){
                        System.out.println("SENDING");
                    } else if (m.getType() == MessageType.END){
                        System.out.println("END");
                        System.exit(0);
                    }
                } catch (InterruptedException e){
                    System.err.println("Failed to take from queue: "+e);
                }
            }
        }
    }

    public static void main(String args[]) {
        if(args.length > 0){
            frequency = Integer.parseInt(args[0]);
        }
        new MyProtocol(SERVER_IP, SERVER_PORT, frequency);
    }
}