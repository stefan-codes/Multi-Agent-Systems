package agents;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

public class ClientAgent extends Agent {

    // Called when the agent is being created
    @Override
    protected void setup() {
        System.out.println("Hello world! Agent " + getAID().getLocalName() + " is ready!");

        // Register for Yellow pages
        registerWithDFAgent("client", "-client-agent");


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

    // Register with YellowPages
    private void registerWithDFAgent(String type, String nameExtension){
        // Register with the DF agent for the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        ServiceDescription sd = new ServiceDescription();
        sd.setType(type);
        sd.setName(getLocalName() + nameExtension);

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
