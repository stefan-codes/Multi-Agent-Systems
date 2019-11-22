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
    private int day = 1;
    private int agentsReady = 0;
    private boolean waiting = false;
    private boolean doneForToday = false;

    private ArrayList<AID> manufacturers = new ArrayList<>();
    private ArrayList<AID> clients = new ArrayList<>();
    private ArrayList<AID> suppliers = new ArrayList<>();

    @Override
    protected void setup() {
        System.out.println("Hello, " + getLocalName() + " is starting...");

        addBehaviour(new RegisterWithDFAgent(this));

        // Process the arguments
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
        doWait(5000);

        // Add behaviours
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

        private ArrayList<AID> agentsInTheSimulation = new ArrayList<>();

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
                    if (agentsReady >= agentsInTheSimulation.size()){
                        doneForToday = true;
                    }
                } else {
                    block();
                }
            } else {
                addBehaviour(new UpdateAgentList(myAgent));

                // Send "new day" message to all agents
                ACLMessage message = new ACLMessage(ACLMessage.INFORM);
                message.setContent("new day");
                for(AID id : agentsInTheSimulation){
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
        public void reset() {
            super.reset();
            agentsInTheSimulation.clear();
            doneForToday = false;
            waiting = false;
            agentsReady = 0;
            day++;

        }

        @Override
        public int onEnd() {
            System.out.println("End of day " + day);
            if (day == daysToAchieve) {
                // Tell the agents we are done with the simulation
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.setContent("finished");

                // Send to all agents
                for (AID id : agentsInTheSimulation) {
                    msg.addReceiver(id);
                }

                myAgent.send(msg);
                myAgent.doDelete();
            } else {
                // Otherwise reset and move to next day
                reset();
                myAgent.addBehaviour(this);
            }

            return 0;
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
    private class UpdateAgentList extends OneShotBehaviour {

        UpdateAgentList(Agent agent){
            super(agent);
        }

        @Override
        public void action() {
            // Create descriptions for each type of agent in the system
            DFAgentDescription clientAD = new DFAgentDescription();
            ServiceDescription clientSD = new ServiceDescription();
            clientSD.setType("client");

            DFAgentDescription manufacturerAD = new DFAgentDescription();
            ServiceDescription manufacturerSD = new ServiceDescription();
            manufacturerSD.setType("manufacturer");

            DFAgentDescription supplierAD = new DFAgentDescription();
            ServiceDescription supplierSD = new ServiceDescription();
            supplierSD.setType("supplier");

            // Try to find all agents and add them to the list
            try {
                DFAgentDescription[] clientAgents = DFService.search(myAgent, clientAD);
                for (DFAgentDescription client : clientAgents) {
                    clients.add(client.getName());
                }

                DFAgentDescription[] manufacturerAgents = DFService.search(myAgent, manufacturerAD);
                for (DFAgentDescription manufacturer : manufacturerAgents) {
                    manufacturers.add(manufacturer.getName());
                }

                DFAgentDescription[] supplierAgents = DFService.search(myAgent, supplierAD);
                for (DFAgentDescription supplier : supplierAgents) {
                    suppliers.add(supplier.getName());
                }

            }
            catch (FIPAException e) {
                e.printStackTrace();
            }
        }
    }
}

