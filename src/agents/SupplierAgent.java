package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;

public class SupplierAgent extends Agent {

    private AID simulation;
    private ArrayList<AID> manufacturers = new ArrayList<>();

    // Called when the agent is being created
    @Override
    protected void setup() {
        // Register with DF
        addBehaviour(new RegisterWithDFAgent(this));

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
            //TODO: System.out.println(myAgent.getAID().getLocalName() + " is listening...");
            MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchContent("new day"),
                    MessageTemplate.MatchContent("finished"));
            ACLMessage msg = myAgent.receive(mt);

            if(msg != null) {
                if(simulation == null) {
                    simulation = msg.getSender();
                }
                if(msg.getContent().equals("new day")) {
                    // TODO: System.out.println("New day registered - " + myAgent.getAID().getLocalName());
                    // New Sequential behaviour for daily activities
                    SequentialBehaviour dailyActivity = new SequentialBehaviour();

                    // Add sub-behaviours (executed in the same order)
                    dailyActivity.addSubBehaviour(new UpdateAgentList(myAgent));
                    // TODO: add more behaviours like make order etc.
                    dailyActivity.addSubBehaviour(new EndDay(myAgent));

                    myAgent.addBehaviour(dailyActivity);
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
            sd.setType("supplier");
            sd.setName(getLocalName() + "-supplier-agent");

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
            DFAgentDescription manufacturerAD = new DFAgentDescription();
            ServiceDescription manufacturerSD = new ServiceDescription();
            manufacturerSD.setType("manufacturer");

            // Try to find all agents and add them to the list
            try {
                DFAgentDescription[] manufacturerAgents = DFService.search(myAgent, manufacturerAD);
                for (DFAgentDescription manufacturer : manufacturerAgents) {
                    manufacturers.add(manufacturer.getName());
                }

            }
            catch (FIPAException e) {
                e.printStackTrace();
            }
        }
    }

    // Execute at the end of my daily activities
    private class EndDay extends OneShotBehaviour {
        // TODO: update later because its not end of the day?
        EndDay(Agent a) {
            super(a);
        }

        @Override
        public void action() {

            // Tell the simulation that I am done for today
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(simulation);
            msg.setContent("done");
            myAgent.send(msg);
        }
    }
}
