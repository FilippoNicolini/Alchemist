package it.unibo.alchemist.model.implementations.actions;


import it.unibo.alchemist.model.interfaces.Action;
import it.unibo.alchemist.model.interfaces.Node;
import it.unibo.alchemist.model.interfaces.Reaction;
import org.apache.commons.math3.random.RandomGenerator;

/**
 * Agent used to deliver messages between other agents.
 */
public class PostmanAgent extends AbstractAgent {

    /**
     * Constructor for the agent.
     * @param name name of the agent.
     * @param node node where the agent is placed.
     * @param rand random generator.
     */
    public PostmanAgent(final String name, final Node<Object> node, final RandomGenerator rand) {
        super(name, node, rand);
    }

    /**
     * Clone the action.
     * @param node the node which the action is referred.
     * @param reaction the reaction which the action is referred.
     * @return a new PostmanAgent.
     */
    @Override
    public Action<Object> cloneAction(final Node<Object> node, final Reaction<Object> reaction) {
        return new PostmanAgent("cloned_" + this.getAgentName(), node, this.getAgentRandomGenerator());
    }

    /**
     * Execute the PostmanAgent reasoning cycle.
     */
    @Override
    public void execute() {
        getNode().postman();
    }

}
