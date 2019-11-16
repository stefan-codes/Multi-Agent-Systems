package defaultPackage;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;

public class Main {

    public static void main(String[] args) {
        //Setup the JADE environment
        Profile myProfile = new ProfileImpl();
        Runtime myRuntime = Runtime.instance();
        ContainerController myContainer = myRuntime.createMainContainer(myProfile);

        try{
            //Start the agent controller, which is itself an agent (rma)
            AgentController rma = myContainer.createNewAgent("rma", "jade.tools.rma.rma", null);
            rma.start();

            /*
            // Pass in books to sell
            String[] books = {"java"};

            //Now start my Agents
            AgentController actioneer = myContainer.createNewAgent("actioneer", Auctioneer.class.getCanonicalName(), null);
            actioneer.start();

            AgentController bidder = myContainer.createNewAgent("buyerA", Bidder.class.getCanonicalName(), books);
            bidder.start();
            */

        }catch(Exception e) {
            System.out.println("Exception starting agent: " + e.toString());
        }
    }
}
