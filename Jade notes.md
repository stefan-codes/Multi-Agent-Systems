# Multi-Agent Systems notes using JADE with Java
The JADE jar can be downloaded from:

https://jade.tilab.com/download/jade/

Documentation on JADE:

https://jade.tilab.com/doc/api/index.html

Recommended book:

Bellifemine, Fabio Luigi, et al. Developing Multi-Agent Systems with JADE. ProQuest, 2007.

## Basics

### Jade Set Up
```Java
public static void main(String[] args) {
    // Setup the JADE environment
    Profile myProfile = new ProfileImpl();
    Runtime myRuntime = Runtime.instance();
    ContainerController myContainer = myRuntime.createMainContainer(myProfile);
}
```

### setup()

The setup method is called once at the time of creation of the agent.

```Java
@override
protected void setup() {}
```

### takeDown()

The takeDown method is called once right before an agent is deleted. If you need to do any house keeping before destroying an agent - do it here.

```Java
@override
protected void takeDown() {}
```

### doDelete()

The doDelete mathod kills an agent. This method automatically calls the takeDown method.

```Java
myAgent.doDelete();
```



## Types of Agents

### Remote Management Agent (RMA)

It is created like anyother agents and is used to register any new agents to it. That way we can keep track of "alive" agents in the system.

```Java
try {
    // Start the agent controller, which is itself an agent (rma)
    AgentController rma = myContainer.createNewAgent("rma", "jade.tools.rma.rma", null);
    rma.start();
} 
catch (Exception e) {
    System.out.println("Exception starting agent: " + e.toString());
}
```

### The Directory Facilitator (DF)

It is used to register and deregister agents with the yellow pages service. It is created by default and we

#### Register ####

This is usually done in the **setup()**. Often, is recommended to register your agent with the DF agent.

```Java
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
```

#### Deregister

This is usually done in the **takeDown()**. It is important to not forget to deregister.

```Java
// Deregister the agent! its important to do!
try {
    DFService.deregister(this);
}
catch (FIPAException e) {
    e.printStackTrace();
}
```

## Behaviours

Behaviours are like methods in OOP but they run concurrently. When you need to define a functionality, do it in a behaviour. Defined in setup you can create your own behaviours. There are few pre-defined ones.

**action()**

During action, all other behvaiours are being hold on wait in a queue. After all are done, it loops back to the first.

**done()**

At the end of action, done is being executed. When done returns true, it removes the behaviours from the queue.

**onEnd()**

When done returns true, and the behaviour is being removed, onEnd is ran and it returns an int which is the status code (0 for normal).


When defining Behvaiours as classes, it is a common practice in JADE to make them inner classes so they can access private features of the Agent.

### TickerBehaviour

This behaviour is used to exicute its body on a timer. The example uses **onTick()**.
```Java
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
```

### Switching Behaviours

Since the action runs until the done returns true, we can put on wait other behaviours. Be careful to not have long computation or infinate loops inside action.

```Java
// Called when the agent is being created
@Override
protected void setup() {
    System.out.println("Hello world! Agent " + getAID().getLocalName() + " is ready!");

    registerWithDFAgent();

    addBehaviour(new BehaviourOne(this));
    addBehaviour(new BehaviourTwo(this));

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
```

### OneShotBehaviour

This type of behaviour has its done() method to always return true. That way it is ran only once.

```Java
public class OSB extends OneShotBehaviour {
    @Override
    public void action(){
        System.out.println("Hello and Bye!");
    }
}
```

### SequentialBehaviour

Since we have to wait till a behaviour's action is complete, until we move to the next action, if we can a complex long logic we will block the use of other behaviours. To avoid that we want to break a process into smaller chuncks. That can be done by creating a class for each small chunck and add it as a sub-behaviour.

```Java
public class SequentialAgent extends Agent{
    @Override
    protected void setup(){
        System.out.println("Hello!");

        SequentialBehaviour s1 = new SequentialBehaviour(this);
        s1.addSubBehaviour(new OSB());
        addBehaviour(s1);
    }
}

public class OSB extends OneShotBehaviour {
    @Override
    public void action(){
        System.out.println("Hello and Bye!");
    }
}
```

Sometimes we want to **reuse behaviours** to avoid creating new objects and having the garbage collector dealing with the old ones. To do this we reset the state of our behaviour and modify onEnd to re-insert the behaviour back in the queue.

```Java
public class MyBehaviour extends Behaviour {

    @Override
    public int onEnd() {
        // Use the reset to deal with its state
        reset();
        
        // Dont forget to add it back in the queue
        myAgent.addBehaviour(this);

        return 0;
    }

    @Override
    public void reset() {
        // Update the variables that keep track of the state e.g.
        finished = false;
    }
}
```

### CyclicBehaviour

This behaviour runs forever. It has its done method modified to return false. It is final and subclasses cant change that.

```Java
public class MyBehaviour extends CyclicBehaviour {
    @Override
    public void action(){
        // Do your logic
    }
}
```

### WakerBehaviour

### ParallelBehaviour

### FiniteStateMachineBehaviour

## Communication between agents
