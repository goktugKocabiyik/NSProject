import java.util.Scanner;

public class ApplicationLayer {

    // The host to connect to. Set this to localhost when using the audio interface tool.
    private static String SERVER_IP = "netsys2.ewi.utwente.nl"; //"127.0.0.1";
    // The port to connect to. 8954 for the simulation server.
    private static int SERVER_PORT = 8954;
    // The frequency to use.
    private static int frequency = 5400;

    private int ID;


    public ApplicationLayer () {

//            if(args.length > 0){
//                frequency = Integer.parseInt(args[0]);
//            }


        Scanner in = new Scanner(System.in);

        while (true) {
            System.out.println("Enter your ID: ");
            if (in.hasNextInt()) {
                ID = in.nextInt();
                break;
            }
        }

        new MyProtocol(SERVER_IP, SERVER_PORT, frequency);
    }


    public void runChat() {
        // handle sending from stdin from this thread.



    }


}
