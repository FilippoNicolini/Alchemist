package it.unibo.alchemist.model.implementations.nodes;

import it.unibo.alchemist.model.implementations.actions.SimpleAgentAction;
import it.unibo.alchemist.model.implementations.environments.Continuous2DEnvironment;
import it.unibo.alchemist.model.implementations.times.DoubleTime;
import it.unibo.alchemist.model.interfaces.Environment;
import it.unibo.alchemist.model.interfaces.Position;
import it.unibo.alchemist.model.interfaces.Time;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple node for Agent Incarnation
 */
public class AgentsContainerNode extends AbstractNode<Object> {

    private final String param;
    private final Environment<Object, Position<? extends Continuous2DEnvironment>> environment;
    private Map<String, SimpleAgentAction> agents = new LinkedHashMap<>();

    private Integer directionAngle = 90; // [0-360] direction zero means to point to the north
    private Double speed = 0.01; // speed zero means that the node is stopped
    private Time lastPositionUpdateTau;

    /**
     * Constructor for the agents container.
     * @param p is the param from the simulation configuration file
     * @param env
     */
    public AgentsContainerNode(final String p, final Environment<Object, Position<? extends Continuous2DEnvironment>> env) {
        super(env);
        this.environment = env;
        this.param = p;
        this.lastPositionUpdateTau = new DoubleTime(); // initialize the last update to time zero
        System.out.println("ENVIRONMENT CLASS: " + this.environment.getClass().toString());
    }

    @Override
    protected Object createT() {
        return new AgentsContainerNode(this.param, this.environment);
    }

    // TODO verificare sincronizzazione
    public void setNodeDirection(final int angle) {
        this.directionAngle = angle;
    }

    public void setNodeSpeed(final double speed) {
        this.speed = speed;
    }

    /*public String getParam() {
        return param;
    }*/

    /*public Environment<Object, Position<? extends Continuous2DEnvironment>> getEnvironment() {
        return environment;
    }*/

    /*public SimpleAgentAction getAgent(final String agentName) {
        return this.agents.get(agentName);
    }*/

    /**
     * Add agent reference to the map
     * @param agent
     */
    public void addAgent(final SimpleAgentAction agent) {
        this.agents.put(agent.getAgentName(), agent);
    }

    /**
     * Get the map that contains references of the agents
     * @return
     */
    public Map<String, SimpleAgentAction> getAgentsMap() {
        return this.agents;
    }

    /**
     * Use the environment to move the node. The new position is obtained by a circle whose center is the current position and radius the calculated distance (Time * Speed).
     * The direction (or angle) defines which point on the circumference will be the next node position.
     * @param updateTau tau of the update
     */
    public void changeNodePosition(final Time updateTau) {
        final Position currentPosition = this.environment.getPosition(this);
        final double radAngle =  (90 - this.directionAngle) * Math.PI / 180; // convert degrees to radians (- 90 is the correction angle)
        final double radius = (updateTau.toDouble() - this.lastPositionUpdateTau.toDouble()) * this.speed; // radius = space covered = time spent * speed
        final Number x = currentPosition.getCoordinate(0) + radius * Math.cos(radAngle);
        final Number y = currentPosition.getCoordinate(1) + radius * Math.sin(radAngle);
        // TODO per calcolare la y si deve considerare la direzione dell'asse delle ascisse: se aumenta verso l'altro usare il +, altrimenti usare il -

        this.environment.moveNodeToPosition(this, this.environment.makePosition(x, y));
        this.lastPositionUpdateTau = updateTau;
    }

    public void postman() {
        final Map<String, SimpleAgentAction> tmpAgentMap = new LinkedHashMap<>();

        // obtains, for each node in the environment, all its agents and places them on a single map
        this.environment.getNodes().forEach(n -> {
            tmpAgentMap.putAll(((AgentsContainerNode)n).getAgentsMap());
        });

        // for each agent takes outgoing messages and sends them to recipients
        tmpAgentMap.forEach((agentName, agent) -> {
            final List outMessages = agent.consumeOutgoingMessages();
//            if (outMessages.size() > 0) {
//                System.out.println("-----------------------------------------------");
//                System.out.println("MESSAGE OUTCOMING FROM " + agentName);
//                outMessages.forEach(m -> {
//                    System.out.println(
//                            "Sender: " + ((AgentAction.OutMessage) m).getSender() +
//                                    " || Receiver: " + ((AgentAction.OutMessage) m).getReceiver() +
//                                    " || Payload: " + ((AgentAction.OutMessage) m).getPayload()
//                    );
//                });
//            }
            tmpAgentMap.values().forEach(a -> a.receiveIncomingMessages(outMessages));
        });
    }
}
