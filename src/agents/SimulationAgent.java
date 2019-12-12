package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.ArrayList;

public class SimulationAgent extends Agent {
    private int daysToAchieve = 0;
    private int day = 0;
    private int agentsReady = 0;
    private int agentsInTheSimulation = 0;
    private boolean waiting = false;
    private boolean doneForToday = false;

    private AID manufacturer;
    private ArrayList<AID> customers = new ArrayList<>();
    private ArrayList<AID> suppliers = new ArrayList<>();

    @Override
    protected void setup() {
        System.out.println("Hello, " + getLocalName() + " is starting...");

        // Register with the DF agent
        addBehaviour(new RegisterWithDFAgent(this));

        // Set the target days
        Object[] args = getArguments();
        if (args != null && args.length>0) {
            try {
                daysToAchieve = (int) args[0];
            }
            catch (NumberFormatException nfe){
                nfe.printStackTrace();
            }

        }

        // Wait for rest of the agents to load
        doWait(15000);

        // Start the synchronizer
        addBehaviour(new SyncAgentsBehaviour(this));
    }

    // Called when the agent is being decommissioned
    @Override
    protected void takeDown() {
        // Deregister the agent! its important to do!
        try {
            DFService.deregister(this);
        }
        catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    // Synchronizing behaviour that ticks the days
    private class SyncAgentsBehaviour extends Behaviour {

        SyncAgentsBehaviour(Agent agent){
            super(agent);
        }

        @Override
        public void action() {

            // If we are waiting for responses
            if (waiting){
                MessageTemplate mt = MessageTemplate.MatchContent("done");
                ACLMessage msg = myAgent.receive(mt);
                if (msg != null) {
                    agentsReady++;
                    // When we have all agents respond with done, move to next day
                    if (agentsReady >= agentsInTheSimulation){
                        doneForToday = true;
                    }
                } else {
                    block();
                }
            } else {
                day++;
                UpdateAgentList(myAgent);

                // Send "new day" message to all agents
                ACLMessage message = new ACLMessage(ACLMessage.INFORM);
                message.setConversationId("synchronizer");
                message.setContent(String.valueOf(day));

                // Add all customers
                for(AID id : customers){
                    message.addReceiver(id);
                }

                // Add the manufacturer
                message.addReceiver(manufacturer);

                // Add all suppliers
                for(AID id : suppliers){
                    message.addReceiver(id);
                }

                myAgent.send(message);

                // Go to waiting stage
                waiting = true;
            }


        }

        @Override
        public boolean done() {
            return doneForToday;
        }

        @Override
        public int onEnd() {
            System.out.println("End of day " + day);
            if (day == daysToAchieve) {
                // Tell the agents we are done with the simulation
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.setConversationId("synchronizer");
                msg.setContent("terminate");

                // Send to all agents
                for (AID id : customers) {
                    msg.addReceiver(id);
                }

                msg.addReceiver(manufacturer);

                for (AID id : suppliers) {
                    msg.addReceiver(id);
                }

                myAgent.send(msg);
                System.out.println("Simulation complete.");
                myAgent.doDelete();
            } else {
                // Otherwise reset and move to next day
                reset();
                myAgent.addBehaviour(this);
            }

            return 0;
        }

        @Override
        public void reset() {
            super.reset();
            agentsInTheSimulation = 0;
            doneForToday = false;
            waiting = false;
            agentsReady = 0;
            manufacturer = null;
            customers.clear();
            suppliers.clear();
        }

    }

    // Register the agent with the DF agent for Yellow Pages
    private class RegisterWithDFAgent extends OneShotBehaviour {

        RegisterWithDFAgent(Agent agent){
            super(agent);
        }

        @Override
        public void action() {
            // Register with the DF agent for the yellow pages
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());

            ServiceDescription sd = new ServiceDescription();
            sd.setType("simulation");
            sd.setName(getLocalName() + "-simulation-agent");

            dfd.addServices(sd);

            try {
                DFService.register(myAgent, dfd);
            }
            catch (FIPAException e) {
                e.printStackTrace();
            }
        }
    }

    // Update the list of agents in the simulation
    private void UpdateAgentList(Agent agent) {
        // Create descriptions for each type of agent in the system
        DFAgentDescription customerAD = new DFAgentDescription();
        ServiceDescription customerSD = new ServiceDescription();
        customerSD.setType("customer");
        customerAD.addServices(customerSD);

        DFAgentDescription manufacturerAD = new DFAgentDescription();
        ServiceDescription manufacturerSD = new ServiceDescription();
        manufacturerSD.setType("manufacturer");
        manufacturerAD.addServices(manufacturerSD);

        DFAgentDescription supplierAD = new DFAgentDescription();
        ServiceDescription supplierSD = new ServiceDescription();
        supplierSD.setType("supplier");
        supplierAD.addServices(supplierSD);

        // Try to find all agents and add them to the list
        try {
            DFAgentDescription[] customerAgents = DFService.search(agent, customerAD);
            for (DFAgentDescription customer : customerAgents) {
                customers.add(customer.getName());

            }

            DFAgentDescription[] manufacturerAgents = DFService.search(agent, manufacturerAD);
            for (DFAgentDescription m : manufacturerAgents) {
                manufacturer = m.getName();
            }

            DFAgentDescription[] supplierAgents = DFService.search(agent, supplierAD);
            for (DFAgentDescription supplier : supplierAgents) {
                suppliers.add(supplier.getName());
            }

            agentsInTheSimulation = customers.size() + 1 + suppliers.size();

        }
        catch (FIPAException e) {
            e.printStackTrace();
        }
    }

}

