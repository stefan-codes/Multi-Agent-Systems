package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;

public class ManufacturerAgent extends Agent {

    private AID simulation;
    private ArrayList<AID> suppliers = new ArrayList<>();
    private ArrayList<AID> clients = new ArrayList<>();

    // Called when the agent is being created
    @Override
    protected void setup() {
        System.out.println("Hello, " + getLocalName() + " is starting...");

        // Register with DF
        addBehaviour(new RegisterWithDFAgent(this));

        // Update simulation AID
        addBehaviour(new FindSimulationInstance(this));

        // Add listener behaviour
        addBehaviour(new WaitForNewDay(this));
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

    // A behaviour to way for a new day - MAIN behaviour
    private class WaitForNewDay extends CyclicBehaviour {

        // Constructor with an agent
        WaitForNewDay(Agent agent){
            super(agent);
        }

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchContent("new day"),
                    MessageTemplate.MatchContent("finished"));
            ACLMessage msg = myAgent.receive(mt);

            if(msg != null) {
                if(msg.getContent().equals("new day")) {
                    // Update the list of agents in the system
                    addBehaviour(new UpdateAgentList(myAgent));
                    /*myAgent.addBehaviour(new BookGenerator());
                    myAgent.addBehaviour(new FindBuyers(myAgent));
                    CyclicBehaviour os = new OffersServer(myAgent);
                    myAgent.addBehaviour(os);
                    ArrayList<Behaviour> cyclicBehaviours = new ArrayList<>();
                    cyclicBehaviours.add(os);
                    myAgent.addBehaviour(new EndDayListener(myAgent,cyclicBehaviours));*/
                }
                else {
                    // message to end simulation
                    myAgent.doDelete();
                }
            }
            else{
                block();
            }
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
            sd.setType("manufacturer");
            sd.setName(getLocalName() + "-manufacturer-agent");

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

            DFAgentDescription supplierAD = new DFAgentDescription();
            ServiceDescription supplierSD = new ServiceDescription();
            supplierSD.setType("supplier");

            // Try to find all agents and add them to the list
            try {
                DFAgentDescription[] clientAgents = DFService.search(myAgent, clientAD);
                for (DFAgentDescription client : clientAgents) {
                    clients.add(client.getName());
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

    // Set the simulation instance
    private class FindSimulationInstance extends OneShotBehaviour {

        FindSimulationInstance(Agent agent){
            super(agent);
        }

        @Override
        public void action() {
            // Create descriptions for each type of agent in the system
            DFAgentDescription simulationAD = new DFAgentDescription();
            ServiceDescription simulationSD = new ServiceDescription();
            simulationSD.setType("simulation");

            try {
                DFAgentDescription[] simulationAgents = DFService.search(myAgent, simulationAD);
                simulation = simulationAgents[0].getName();
            }
            catch (FIPAException e) {
                e.printStackTrace();
            }
        }
    }
}
