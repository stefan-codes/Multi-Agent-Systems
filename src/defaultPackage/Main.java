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
        int numberOfCustomers = 3;
        int numberOfSuppliers = 2;

        try{
            // Start the agent controller, which is itself an agent (rma)
            AgentController rma = myContainer.createNewAgent("rma", "jade.tools.rma.rma", null);
            rma.start();

            // Start the customer Agents
            for (int i = 0; i < numberOfCustomers; i++) {
                AgentController customer = myContainer.createNewAgent("Customer"+i, CustomerAgent.class.getCanonicalName(), null);
                customer.start();
            }

            // Start the Manufacturer Agent
            AgentController manufacturer = myContainer.createNewAgent("Manufacturer", ManufacturerAgent.class.getCanonicalName(), null);
            manufacturer.start();

            // Suppliers
            for (int i = 0; i < numberOfSuppliers; i++) {
                SupplierManufacturer.CreateSupplier(myContainer, "Supplier"+i, i);
            }

            // Start the Simulation Agent
            AgentController simulation = myContainer.createNewAgent("Simulation", SimulationAgent.class.getCanonicalName(), daysToRun);
            simulation.start();


        }catch(Exception e) {
            System.out.println("Exception starting agent: " + e.toString());
        }
    }
}
