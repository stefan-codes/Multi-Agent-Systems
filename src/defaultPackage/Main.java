package defaultPackage;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import agents.*;

public class Main {

    public static void main(String[] args) {
        // Setup the JADE environment
        Profile myProfile = new ProfileImpl();
        Runtime myRuntime = Runtime.instance();
        ContainerController myContainer = myRuntime.createMainContainer(myProfile);

        // Variables for the simulation
        int daysToRun = 100;
        int numberOfClients = 3;

        //
        //ArrayList<AgentController> client


        try{
            // Start the agent controller, which is itself an agent (rma)
            AgentController rma = myContainer.createNewAgent("rma", "jade.tools.rma.rma", null);
            rma.start();

            // Start the clients
            for (int i = 0; i < numberOfClients; i++) {
                AgentController client = myContainer.createNewAgent("Client"+i, ClientAgent.class.getCanonicalName(), null);
                client.start();
            }

            // Start the Manufacturer
            AgentController manufacturer = myContainer.createNewAgent("Manufacturer", ManufacturerAgent.class.getCanonicalName(), null);
            manufacturer.start();

            // Start the Suppliers


        }catch(Exception e) {
            System.out.println("Exception starting agent: " + e.toString());
        }
    }
}
