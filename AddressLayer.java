import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class AddressLayer {
    private int MAX_ADDRESS_BITS = 10;
    private int bigAddress;
    private int smallAddress;

    private List<RoutingColumn> routingTable;

    private CSMACA csmaca;

    private int ID;

    public AddressLayer(CSMACA csmaca, int ID) {
        routingTable = new ArrayList<>();
        this.csmaca = csmaca;

        this.ID = ID;

        startUp();
    }

    public synchronized void startUp() {
        // assign a big random address
//        bigAddress = (int) (Math.random() * (Math.pow(2, MAX_ADDRESS_BITS) - 1));

        // add yourself to the routing table
        routingTable.add(new RoutingColumn(ID, ID, 0));

        // spread the table
        TableDistribution td = new TableDistribution(csmaca);
        td.start();
    }

    public synchronized ByteBuffer createTableUpdate() {
        ByteBuffer tableUpdate = ByteBuffer.allocate(16);

        tableUpdate.put((byte) (0x10 | ID));

        tableUpdate.put((byte) (routingTable.size() * 2));

        for (int i = 0; i < routingTable.size(); i++) {
            tableUpdate.put(routingTable.get(i).convert());
        }

        return tableUpdate;
    }

    public synchronized void updateTable (byte[] receivedTable) {

        int source = receivedTable[0] & 15;

        int length = receivedTable[1];

        for (int i = 2; i < length + 1; i += 2) {
            RoutingColumn rc = new RoutingColumn (new byte[]{receivedTable[i], receivedTable[i + 1]});

            boolean destAlreadyInTable = false;

            for (int j = 0; j < routingTable.size(); j++) {

                if (routingTable.get(j).getDestination() == rc.destination) {
                    destAlreadyInTable = true;

                    if (routingTable.get(j).getHops() > (rc.hops + 1)) {
                        routingTable.get(j).hops = rc.hops + 1;
                        routingTable.get(j).nextHop = source;
                    }

//                    System.out.println("Found entry in the table");

                }
            }

            if (!destAlreadyInTable) {
                // put it in a table
                rc.setHops(rc.hops + 1);
                rc.setNextHop(source);

//                System.out.println("ADDED");
                routingTable.add(rc);
            }
        }
    }

    public class RoutingColumn {
        private int destination;
        private int nextHop;
        private int hops;

        public RoutingColumn(int destination, int nextHop, int hops) {
            this.destination = destination;
            this.nextHop = nextHop;
            this.hops = hops;
        }

        public int getDestination() {
            return destination;
        }

        public void setDestination(int destination) {
            this.destination = destination;
        }

        public int getNextHop() {
            return nextHop;
        }

        public void setNextHop(int nextHop) {
            this.nextHop = nextHop;
        }

        public int getHops() {
            return hops;
        }

        public void setHops(int hops) {
            this.hops = hops;
        }

        public byte[] convert() {
//            byte[] result = new byte[3];
            byte[] result = new byte[2];

//            result[0] = (byte) (destination >> 2);
//            result[1] = (byte) (((destination & 3) << 6) + (hops << 2) + ((nextHop >> 8) & 3));
//            result[2] = (byte) nextHop;

            result[0] = (byte) ((destination << 4) + nextHop);
            result[1] = (byte) (hops);
//            result[2] = (byte) nextHop;

            return result;
        }

        public RoutingColumn (byte[] update) {
            // destination, hops, nextHop
//            this(((((update[0] + 256) & 255) << 2)) + ((update[1] >> 6) & 3),((update[1] & 3) << 8) + update[2],(update[1] >> 2) & 15);
            this(((((update[0] + 256) & 255) >> 4)),update[0] & 15,update[1]);
        }
    }

    private class TableDistribution extends Thread {
        private CSMACA csmaca;

        public TableDistribution (CSMACA csmaca) {
            this.csmaca = csmaca;
        }

        public void run () {

            while (true) {
                ByteBuffer bb = createTableUpdate();

                csmaca.sendMessage(bb);

//                System.out.println(Arrays.toString(bb.array()));
//                printTable();

                try {
                    // (int)(Math.random() * (max - min) + 1) + min
                    Thread.sleep((int) (Math.random() * (1300 - 800) + 1) + 800);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }

            }
        }
    }

    public void printTable () {
        System.out.println();
        for (int i = 0; i < routingTable.size(); i++) {
            System.out.println("ENTRY " + (i + 1));
            System.out.println("Dest: " + routingTable.get(i).destination);
            System.out.println("NextHops: " + routingTable.get(i).nextHop);
            System.out.println("Hops: " + routingTable.get(i).hops);
        }
        System.out.println();
    }

    public static void main(String[] args) {
//        AddressLayer al = new AddressLayer(new CSMACA());
//        RoutingColumn rc = al.new RoutingColumn(128, 256, 10);
//        RoutingColumn rccc = al.new RoutingColumn(rc.convert());
//        System.out.println(rccc.destination);
//        System.out.println(rccc.nextHop);
//        System.out.println(rccc.hops);
//        System.out.println(Arrays.toString(rc.convert()));
    }

    public int getNextHop(int destination) {
        for (int i = 0; i < routingTable.size(); i++) {
            if (routingTable.get(i).getDestination() == destination) {
                return routingTable.get(i).nextHop;
            }
        }

        // if there is no suitable column in the routing table
        return -1;
    }
}
