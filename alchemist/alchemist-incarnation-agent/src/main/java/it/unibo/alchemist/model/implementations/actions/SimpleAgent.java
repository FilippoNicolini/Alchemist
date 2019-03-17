package it.unibo.alchemist.model.implementations.actions;

import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.Theory;
import it.unibo.alchemist.model.interfaces.Reaction;
import it.unibo.alchemist.model.interfaces.Action;
import it.unibo.alchemist.model.interfaces.Node;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Simple implementation of an agent.
 */
public class SimpleAgent extends AbstractAgent {

    /**
     * Constructor for the agent.
     * @param name name of the agent.
     * @param node node where the agent is placed.
     * @param reaction reaction of the agent.
     */
    public SimpleAgent(final String name, final Node<Object> node, final Reaction<Object> reaction) {
        super(name, node, reaction);

        try {
            this.getEngine().addTheory(new Theory(new FileInputStream(new File("alchemist-incarnation-agent/src/main/resources/" + this.getAgentName() + ".pl"))));
        } catch (IOException e) {
            System.err.println(this.getAgentName() + SEPARATOR + IO_MSG);
        } catch (InvalidTheoryException e) {
            System.err.println(this.getAgentName() + SEPARATOR + INVALID_THEORY_MSG);
        }
    }

    @Override
    public Action<Object> cloneAction(final Node<Object> n, final Reaction<Object> r) {
        return new SimpleAgent("cloned_" + this.getAgentName(), getNode(), r);
    }

    @Override
    public void execute() {
        if (!this.isInitialized()) {
            this.inizializeAgent();

            System.out.println("Nodo: " + getNode().getId() + " || agent " + this.getAgentName() + " inizializzato");

            this.initReasoning();
        } else {
            //Agent's reasoning cycle

            this.handleIncomingMessages();

            this.readMessage();

            this.positionUpdate();

            // Tuple space extension
            this.retrieveTuples();

            this.getBeliefBaseChanges();

            this.notifyBeliefBaseChanges();

            this.handleOutGoingMessages();
        }
    }
}