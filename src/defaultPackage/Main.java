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
        Integer[] daysToRun = {100};
        int numberOfClients = 3;
        int numberOfSuppliers = 2;

        try{
            // Start the agent controller, which is itself an agent (rma)
            AgentController rma = myContainer.createNewAgent("rma", "jade.tools.rma.rma", null);
            rma.start();

            // Start the client Agents
            for (int i = 0; i < numberOfClients; i++) {
                AgentController client = myContainer.createNewAgent("Client"+i, ClientAgent.class.getCanonicalName(), null);
                client.start();
            }

            // Start the Manufacturer Agent
            AgentController manufacturer = myContainer.createNewAgent("Manufacturer", ManufacturerAgent.class.getCanonicalName(), null);
            manufacturer.start();

            // Start the Supplier Agents
            for (int i = 0; i < numberOfSuppliers; i++) {
                AgentController supplier = myContainer.createNewAgent("Supplier"+i, SupplierAgent.class.getCanonicalName(), null);
                supplier.start();
            }

            // Start the Simulation Agent
            AgentController simulation = myContainer.createNewAgent("Simulation", SimulationAgent.class.getCanonicalName(), daysToRun);
            simulation.start();


        }catch(Exception e) {
            System.out.println("Exception starting agent: " + e.toString());
        }
    }
}
