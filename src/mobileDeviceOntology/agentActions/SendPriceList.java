package mobileDeviceOntology.agentActions;

import jade.content.AgentAction;
import jade.content.onto.annotations.Slot;
import mobileDeviceOntology.concepts.PriceList;

public class SendPriceList implements AgentAction {

    private PriceList priceList;

    @Slot (mandatory = true)
    public PriceList getPriceList() {
        return priceList;
    }

    public void setPriceList(PriceList priceList) {
        this.priceList = priceList;
    }
}
