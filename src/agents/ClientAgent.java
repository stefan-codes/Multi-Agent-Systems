package agents;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

public class ClientAgent extends Agent {

    // Called when the agent is being created
    @Override
    protected void setup() {
        System.out.println("Hello world! Agent " + getAID().getLocalName() + " is ready!");

        registerWithDFAgent();

        addBehaviour(new BehaviourOne(this));
        addBehaviour(new BehaviourTwo(this));

    }

    // Called when the agent is being commissioned
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

    // Define my own behaviour
    public class BehaviourOne extends Behaviour {

        private int timesCalled = 0;

        BehaviourOne(Agent agent){
            myAgent = agent;
        }


        @Override
        public void action() {
            System.out.println("Called from: " + myAgent.getLocalName());
            timesCalled++;
        }

        @Override
        public boolean done() {
            return timesCalled >= 10;
        }
    }

    // Define my own behaviour
    public class BehaviourTwo extends Behaviour {

        private int timesCalled = 0;

        BehaviourTwo(Agent agent){
            myAgent = agent;
        }


        @Override
        public void action() {
            System.out.println("Called from: " + myAgent.getLocalName());
            timesCalled++;
        }

        @Override
        public boolean done() {
            return timesCalled >= 10;
        }
    }

    // Register with YellowPages
    private void registerWithDFAgent(){
        // Register with the DF agent for the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        ServiceDescription sd = new ServiceDescription();
        sd.setType("Type");
        sd.setName(getLocalName() + "-agent-type");

        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    // Next method
}
