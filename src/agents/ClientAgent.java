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
        System.out.println("Hello world! Agent " + getAID().getName() + "is ready!");

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
}
