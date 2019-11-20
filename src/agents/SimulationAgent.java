package agents;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;

public class SimulationAgent extends Agent {
    private int counter = 15;

    @Override
    protected void setup() {
        Object days = getArguments();
        System.out.println("I am inside");

        // Create a new TickerBehaviour
        addBehaviour(new TickerBehaviour(this, 1000) {
            // Executed every 1000ms

            @Override
            protected void onTick() {
                if ( counter > 0 ) {
                    System.out.println("Ticking: " + counter);
                    counter--;
                } else {
                    System.out.println("Bye!");
                    myAgent.doDelete();
                }
            }
        });
    }

}
