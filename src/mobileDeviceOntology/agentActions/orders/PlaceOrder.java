package mobileDeviceOntology.agentActions.orders;

import jade.content.AgentAction;
import jade.content.onto.annotations.Slot;
import jade.core.AID;


public class PlaceOrder implements AgentAction {


    private int dayPlaced;

    /* Getters */


    @Slot (mandatory = true)
    public int getDayPlaced() {
        return dayPlaced;
    }



    /* Setters */

    public void setDayPlaced(int dayPlaced) {
        this.dayPlaced = dayPlaced;
    }

}
