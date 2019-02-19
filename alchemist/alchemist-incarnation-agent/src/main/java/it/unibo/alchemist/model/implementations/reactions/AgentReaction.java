package it.unibo.alchemist.model.implementations.reactions;

import it.unibo.alchemist.model.interfaces.Environment;
import it.unibo.alchemist.model.interfaces.Node;
import it.unibo.alchemist.model.interfaces.Time;
import it.unibo.alchemist.model.interfaces.TimeDistribution;
import it.unibo.alchemist.model.interfaces.Reaction;

public class AgentReaction extends AbstractReaction<Object> {

    private final String agentReactionName;

    public AgentReaction(final String agentReactionName, final Node node, final TimeDistribution<Object> time) {
        super(node, time);
        this.agentReactionName = agentReactionName;
    }

    public String getAgentReactionName() {
        return this.agentReactionName;
    }

    @Override
    public void updateInternalStatus(final Time curTime, final boolean executed, final Environment<Object, ?> env) {
        // TODO come implementare?
    }

    @Override
    public Reaction<Object> cloneOnNewNode(final Node<Object> node, final Time currentTime) {
        return new AgentReaction("cloned_" + this.getAgentReactionName(),node,getTimeDistribution());
    }

    @Override
    public double getRate() {
        return getTimeDistribution().getRate();
    }
}
