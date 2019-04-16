package it.unibo.alchemist.model.implementations.actions;

import it.unibo.alchemist.model.implementations.molecules.SimpleMolecule;
import it.unibo.alchemist.model.interfaces.Action;
import it.unibo.alchemist.model.interfaces.Node;
import it.unibo.alchemist.model.interfaces.Reaction;
import org.apache.commons.math3.random.RandomGenerator;

public class MovementAgent extends AbstractAgent {

    public MovementAgent(final String name, final Node<Object> node, final RandomGenerator rand, final Reaction<Object> reaction) {
        super(name, node, rand, reaction);

        node.setConcentration(new SimpleMolecule(name), 0);
    }

    @Override
    public Action<Object> cloneAction(final Node<Object> node, final Reaction<Object> reaction) {
        return new MovementAgent("cloned_" + getAgentName(), node, this.getAgentRandomGenerator(), reaction);
    }

    @Override
    public void execute() {
        getNode().changeNodePosition(this.getAgentReaction().getTau());
    }
}
