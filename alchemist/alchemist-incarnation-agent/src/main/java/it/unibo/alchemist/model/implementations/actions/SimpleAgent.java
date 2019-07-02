package it.unibo.alchemist.model.implementations.actions;

import it.unibo.alchemist.model.implementations.molecules.SimpleMolecule;
import it.unibo.alchemist.model.interfaces.Reaction;
import it.unibo.alchemist.model.interfaces.Action;
import it.unibo.alchemist.model.interfaces.Node;
import org.apache.commons.math3.random.RandomGenerator;

/**
 * Simple implementation of an agent.
 */
public class SimpleAgent extends AbstractAgent {

    /**
     * Constructor for the agent.
     * @param name name of the agent.
     * @param node node where the agent is placed.
     * @param reaction reaction of the agent.
     * @param rand random generator.
     */
    public SimpleAgent(final String name, final Node<Object> node, final RandomGenerator rand, final Reaction<Object> reaction) {
        super(name, node, rand, reaction);

        node.setConcentration(new SimpleMolecule(name), 0);
    }

    /**
     * Clone the action.
     * @param node the node which the action is referred.
     * @param reaction the reaction which the action is referred.
     * @return a new SimpleAgent.
     */
    @Override
    public Action<Object> cloneAction(final Node<Object> node, final Reaction<Object> reaction) {
        return new SimpleAgent("cloned_" + this.getAgentName(), node, this.getAgentRandomGenerator(), reaction);
    }

    /**
     * Execute the SimpleAgent reasoning cycle.
     */
    @Override
    public void execute() {
        if (!this.isInitialized()) {
            this.initializeAgent();

            System.out.println("Nodo: " + getNode().getId() + " || agent " + this.getAgentName() + " inizializzato");

            this.initReasoning();
        } else {
            //Agent's reasoning cycle

            this.beliefBaseChanges();

            this.readMessage();

            // Tuple space extension
            this.retrieveTuples();

            this.executeIntention();
        }
    }
}
