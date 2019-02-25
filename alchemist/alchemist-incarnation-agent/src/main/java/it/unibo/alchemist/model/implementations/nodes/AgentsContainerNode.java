package it.unibo.alchemist.model.implementations.nodes;

import it.unibo.alchemist.model.implementations.actions.SimpleAgentAction;
import it.unibo.alchemist.model.implementations.environments.Continuous2DEnvironment;
import it.unibo.alchemist.model.implementations.times.DoubleTime;
import it.unibo.alchemist.model.interfaces.Environment;
import it.unibo.alchemist.model.interfaces.Position;
import it.unibo.alchemist.model.interfaces.Time;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Node for Agent Incarnation
 */
public class AgentsContainerNode extends AbstractNode<Object> {

    private final String param;
    private final Environment<Object, Position<? extends Continuous2DEnvironment>> environment;
    private final Map<String, SimpleAgentAction> agents = new LinkedHashMap<>();

    private Integer nodeDirectionAngle = 0; // [0-360] Direction zero means to point to the north
    private Double nodeSpeed = 0.01; // Speed zero means that the node is stopped
    private Time lastNodePositionUpdateTau;

    /**
     * Constructor for the agents container.
     * @param p is the param from the simulation configuration file
     * @param env
     */
    public AgentsContainerNode(final String p, final Environment<Object, Position<? extends Continuous2DEnvironment>> env) {
        super(env);
        this.environment = env;
        this.param = p;
        this.lastNodePositionUpdateTau = new DoubleTime(); // Initialize the last update to time zero
        System.out.println("ENVIRONMENT CLASS: " + this.environment.getClass().toString());
    }

    @Override
    protected Object createT() {
        return new AgentsContainerNode(this.param, this.environment); // TODO è corretto?
    }

    public synchronized Integer getNodeDirectionAngle() {
        return this.nodeDirectionAngle;
    }

    public synchronized Double getNodeSpeed() {
        return this.nodeSpeed;
    }

    /**
     * Get the current Position of the node in the environment
     * @return
     */
    public Position getNodePosition() {
        return this.environment.getPosition(this);
    }

    // TODO verificare sincronizzazione
    public synchronized void changeDirectionAngle(final int angle, final boolean isDelta) {
        if (isDelta) {
            this.nodeDirectionAngle += angle;
            /*if (this.nodeDirectionAngle > 360) {
                this.nodeDirectionAngle = this.nodeDirectionAngle % 360;
            }*/
        } else {
            this.nodeDirectionAngle = angle;
        }
    }

    // TODO verificare sincronizzazione
    public synchronized void changeNodeSpeed(final double speed) {
        this.nodeSpeed = speed;
    }

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
        final Position currentPosition = this.getNodePosition();
        final double radAngle =  (90 - this.nodeDirectionAngle) * Math.PI / 180; // convert degrees to radians (- 90 is the correction angle)
        final double radius = (updateTau.toDouble() - this.lastNodePositionUpdateTau.toDouble()) * this.nodeSpeed; // radius = space covered = time spent * speed
        final Number x = currentPosition.getCoordinate(0) + radius * Math.cos(radAngle);
        final Number y = currentPosition.getCoordinate(1) + radius * Math.sin(radAngle);
        // TODO per la y considerare la direzione dell'asse delle ordinate: se aumenta verso l'altro usare il +, altrimenti usare il -

        this.environment.moveNodeToPosition(this, this.environment.makePosition(x, y));
        this.lastNodePositionUpdateTau = updateTau;
    }

    /**
     * Called from postman agent. Collects outgoing messages from all agents and deliver them to recipients
     */
    public void postman() {
        final Map<String, SimpleAgentAction> tmpAgentMap = new LinkedHashMap<>();
        final List<SimpleAgentAction.OutMessage> outMessages = new ArrayList<>();

        // For each node in the environment get all its agents
        this.environment.getNodes().forEach(node -> {
            tmpAgentMap.putAll(((AgentsContainerNode)node).getAgentsMap());
        });

        // For each agent takes outgoing messages
        tmpAgentMap.forEach((agentName, agent) -> {
            outMessages.addAll(agent.consumeOutgoingMessages());
        });

        // Send each message to the receiver
        outMessages.forEach(message -> {
            tmpAgentMap.get(message.getReceiver().toString()).addIncomingMessage(message);
        });
    }

    /**
     * Takes the agent name as input and return a Map with the distance from each agent in the environment
     * @param agentName
     * @return a map with distances from other agents
     */
    public Map<String,Double> getNeighborhoodDistances(final String agentName) {
        final Map<String,Double> agentsDistances = new LinkedHashMap<>();

        // For each neighbor node
        this.environment.getNeighborhood(this).getNeighbors().forEach(node -> {
            // Calculates the distance between nodes
            final double distance = this.environment.getDistanceBetweenNodes(this,node);
            // Sets the distance for each agent inside the node
            ((AgentsContainerNode)node).getAgentsMap().keySet().forEach(strNeighborAgentName -> {
                agentsDistances.put(strNeighborAgentName,distance);
                // System.out.println("******Agent " + agentName + " is distance " + distance + " from agent " + strNeighborAgentName);
            });
        });

        return agentsDistances;
    }
}
