package it.unibo.alchemist.model.implementations.actions;

import alice.tuprolog.*;
import alice.tuprolog.Double;
import alice.tuprolog.Number;
import alice.tuprolog.lib.InvalidObjectIdException;
import alice.tuprolog.lib.OOLibrary;
import it.unibo.alchemist.model.implementations.nodes.AgentsContainerNode;
import it.unibo.alchemist.model.interfaces.Context;
import it.unibo.alchemist.model.interfaces.Node;
import it.unibo.alchemist.model.interfaces.Position;
import it.unibo.alchemist.model.interfaces.Reaction;
import kotlin.Triple;
import org.apache.commons.math3.distribution.LevyDistribution;
import org.apache.commons.math3.random.RandomGenerator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;

/**
 * Abstract definition of agent. Contains methods for managing the agent reasoning cycle.
 * Also implemented methods for managing tuple space.
 */
public abstract class AbstractAgent extends AbstractAction<Object> {

    private final static String BASE_THEORY = "agent_library";

    // Messages
    protected final static String IO_MSG = "Failed reading agent's theory file.";
    protected final static String INVALID_THEORY_MSG = "Theory not valid.";
    protected final static String MALFORMED_GOAL_MSG = "Malformed goal";
    protected final static String NO_SOLUTION_MSG = "No solution for goal";
    protected final static String UNKNOWN_VAR_MSG = "Error retrieving the term in the solution of goal";
    protected final static String NO_MORE_SOLUTION_MSG = "Error retrieving next solution of goal";
    protected final static String SEPARATOR = " || ";
    protected final static String SUCCESS_PLAN = "plan done successfully.";
    protected final static String TRIGGERED_PLAN = "Triggered plan";
    protected final static String NO_IMPLEMENTATION_FOUND = "No implementation found for ";
    protected final static String INVALID_OBJECT_MSG = "Invalid object to register into tuProlog engine";

    // Strings used in the code
    private final List<Triple<Struct, String, String>> tripleListForTuples = new ArrayList<>();
    private final String ADD_NOTIFICATION = "add";
    private final String REMOVE_NOTIFICATION = "remove";
    private final String RESPONSE_NOTIFICATION = "response";

    private final String agentName; // String for the agent name
    private boolean isInitialized = false; // Flag for init of the agent
    private final Queue<InMessage> inbox = new LinkedList<>(); // Mailbox IN queue
    private final Queue<OutMessage> outbox = new LinkedList<>(); // Mailbox OUT queue
    private final Prolog engine = new Prolog(); // tuProlog engine
    private Reaction<Object> agentReaction; // Reference to reaction
    private RandomGenerator agentRandomGenerator; // Random generator of the reaction
    private LevyDistribution levyDistribution;
    private final Map<Term, String> beliefBaseChanges = new LinkedHashMap<>(); // Map where to save updated belief notifications

    /**
     * Constructor for abstract agent.
     * @param name agent name.
     * @param node node where the agent is placed.
     * @param rand random generator.
     */
    protected AbstractAgent(final String name, final Node<Object> node, final RandomGenerator rand) {
        super(node);
        this.agentName = name;
        this.agentRandomGenerator = rand;

        this.loadAgentLibrary();
    }

    /**
     * Constructor for abstract agent.
     * @param name agent name.
     * @param node node where the agent is placed.
     * @param rand random generator.
     * @param reaction reaction of the agent.
     */
    protected AbstractAgent(final String name, final Node<Object> node, final RandomGenerator rand, final Reaction<Object> reaction) {
        super(node);
        this.agentName = name;
        this.agentRandomGenerator = rand;
        this.agentReaction = reaction;

        this.loadAgentLibrary();
    }

    /**
     * Get the name of the agent.
     * @return agent name.
     */
    public String getAgentName() {
        return this.agentName;
    }

    /**
     * Get the engine of the agent.
     * @return tuProlog engine.
     */
    protected Prolog getEngine() {
        return this.engine;
    }

    /**
     * Set flag initialized to true for the agent.
     */
    protected void setInitialized() {
        this.isInitialized = true;
    }

    /**
     * Get flag initialized
     * @return true if the agent is initialized.
     */
    protected boolean isInitialized() {
        return this.isInitialized;
    }

    /**
     * Get status of inbox.
     * @return true if inbox is empty.
     */
    private boolean inboxIsEmpty() {
        return this.inbox.size() == 0;
    }

    /**
     * Get the random generator of the agent.
     * @return random generator
     */
    protected RandomGenerator getAgentRandomGenerator() {
        return this.agentRandomGenerator;
    }

    @Override
    public AgentsContainerNode getNode() {
        return (AgentsContainerNode) super.getNode();
    }

    @Override
    public Context getContext() {
        return Context.NEIGHBORHOOD; // TODO va bene come profondità?
    }

    //------------------------------------------
    // Agent's internal action
    //------------------------------------------

    private void loadAgentLibrary() {
        try {
            this.engine.setTheory(new Theory(new FileInputStream(new File("alchemist-incarnation-agent/src/main/resources/" + BASE_THEORY + ".pl"))));
        } catch (IOException e) {
            throw new IllegalArgumentException(this.getAgentName() + SEPARATOR + IO_MSG);
//            System.err.println(this.getAgentName() + SEPARATOR + IO_MSG);
        } catch (InvalidTheoryException e) {
            throw new IllegalArgumentException(this.getAgentName() + SEPARATOR + IO_MSG);
//            System.err.println(this.getAgentName() + SEPARATOR + INVALID_THEORY_MSG);
        }

        try {
            this.levyDistribution = new LevyDistribution(this.agentRandomGenerator, 0, 0.5);
            final Library lib = this.engine.getLibrary("alice.tuprolog.lib.OOLibrary");
            ((OOLibrary) lib).register(new Struct("randomGenerator"), this.agentRandomGenerator);
            ((OOLibrary) lib).register(new Struct("levyDistribution"), this.levyDistribution);
        } catch (InvalidObjectIdException e) {
            throw new IllegalArgumentException(this.getAgentName() + SEPARATOR + INVALID_OBJECT_MSG);
//            System.err.println(this.getAgentName() + SEPARATOR + INVALID_OBJECT_MSG);
        }
    }

    /**
     * Initilizes the agent (puts in the theory his name).
     */
    protected void inizializeAgent() {
        // Name of the agent
        final Struct self = new Struct(
                "self",
                Term.createTerm(this.getAgentName()));

        // Sets 'self' in the theory of the agent
        this.engine.getTheoryManager().assertA(self, true, null, false);

        // Node's starting position
        final Position nodePosition = getNode().getNodePosition();
        final Struct position = new Struct(
                "belief",
                new Struct(
                        "position",
                        new Double(nodePosition.getCoordinate(0)),
                        new Double(nodePosition.getCoordinate(1)))
        );

        // Node's initial speed and direction
        final Struct movement = new Struct(
                "belief",
                new Struct(
                        "movement",
                        new Double(getNode().getNodeSpeed()),
                        new Double(getNode().getNodeDirectionAngle()))
        );

        // Sets the node position
        this.engine.getTheoryManager().assertA(position, true, null, false);

        // Sets the node movement
        this.engine.getTheoryManager().assertA(movement, true, null, false);

        // Sets the beliefs with the distance to other agents
        this.updateAgentsDistances();

        // Prepares list to retrieve tuples
        this.tripleListForTuples.add(new Triple<>(new Struct(
                "retract",
                new Struct(
                        "write",
                        new Var("T"))), "write", "writeTuple"));
        this.tripleListForTuples.add(new Triple<>(new Struct(
                "retract",
                new Struct(
                        "read",
                        new Var("T"))), "read", "readTuple"));
        this.tripleListForTuples.add(new Triple<>(new Struct(
                "retract",
                new Struct(
                        "take",
                        new Var("T"))), "take", "takeTuple"));

        // Updates the initialization flag
        this.setInitialized();
    }

    /**
     * Encapsulates the solution of the initialization plan for the agent.
     */
    protected void initReasoning() {
        // Solves init plan if present
        final SolveInfo init = this.getEngine().solve(new Struct("init"));
        if (init.isSuccess()) {
            // System.out.println(this.getAgentName() + SEPARATOR + "init " + SUCCESS_PLAN);
        } else {
            System.err.println(this.getAgentName() + SEPARATOR + NO_IMPLEMENTATION_FOUND + " init.");
        }
        this.handleOutGoingMessages();
    }

    /**
     * Inserts messages received into the theory.
     */
    protected void handleIncomingMessages() {
        while (!this.inboxIsEmpty()) {
            final InMessage msg = this.inbox.poll();
            if (Objects.nonNull(msg)) {
                final Struct incoming = new Struct(
                        "ingoing",
                        msg.getSender(),
                        msg.getPayload());
                this.engine.getTheoryManager().assertZ(incoming, true, null, false);
            }
        }
    }

    /**
     * Prepares messages to send.
     */
    protected void handleOutGoingMessages() {
        try {
            // Retracts outgoing beliefs in the theory and adds them as messages to the outbox.
            SolveInfo outgoing = this.engine.solve(new Struct(
                    "retract",
                    new Struct(
                            "outgoing",
                            new Var("R"),
                            new Var("S"),
                            new Var("M"))));
            while (outgoing != null && outgoing.isSuccess()) {
                this.outbox.add(new OutMessage(outgoing.getTerm("S"), outgoing.getTerm("R"), outgoing.getTerm("M")));
                if (outgoing.hasOpenAlternatives()) {
                    outgoing = this.engine.solveNext();
                } else {
                    outgoing = null;
                }
            }
        } catch (NoSolutionException e) {
            throw new IllegalStateException(this.getAgentName() + SEPARATOR + NO_SOLUTION_MSG + " outgoing.");
//            System.err.println(this.getAgentName() + SEPARATOR + NO_SOLUTION_MSG + " outgoing.");
        } catch (UnknownVarException e) {
            throw new IllegalStateException(this.getAgentName() + SEPARATOR + UNKNOWN_VAR_MSG + " outgoing.");
//            System.err.println(this.getAgentName() + SEPARATOR + UNKNOWN_VAR_MSG + " outgoing.");
        } catch (NoMoreSolutionException e) {
            throw new IllegalStateException(this.getAgentName() + SEPARATOR + NO_MORE_SOLUTION_MSG + " outgoing.");
//            System.err.println(this.getAgentName() + SEPARATOR + NO_MORE_SOLUTION_MSG + " outgoing.");
        }
    }

    /**
     * Solve receiveMessage plan.
     */
    protected void readMessage() {
        final SolveInfo receive = this.engine.solve(new Struct("receiveMessage"));
        if (receive.isSuccess()) {
            System.out.println(this.getAgentName() + SEPARATOR + this.agentReaction.getTau().toDouble() + SEPARATOR + "receiveMessage " + SUCCESS_PLAN);
        }
    }

    /**
     * Takes changes to the belief base occurred in the reasoning cycle.
     */
    protected void getBeliefBaseChanges() {
        try {
            // Gets removed beliefs
            SolveInfo removeBelief = this.engine.solve(new Struct(
                    "retract",
                    new Struct(
                            "removed_belief",
                            new Var("B"))));
            while (removeBelief != null && removeBelief.isSuccess()) {
                // Puts beliefs in the map for the notification
                this.beliefBaseChanges.put(removeBelief.getTerm("B"), REMOVE_NOTIFICATION);
                if (removeBelief.hasOpenAlternatives()) {
                    removeBelief = this.engine.solveNext();
                } else {
                    removeBelief = null;
                }
            }

            // Gets added beliefs
            SolveInfo insertBelief = this.engine.solve(new Struct(
                    "retract",
                    new Struct(
                            "added_belief",
                            new Var("B"))));
            while (insertBelief != null && insertBelief.isSuccess()) {
                // Puts beliefs in the map for the notification
                final Term currentBelief = insertBelief.getTerm("B");
                this.beliefBaseChanges.put(currentBelief, ADD_NOTIFICATION);

                // Updates node values of speed and direction (that are contained into the belief 'movement')
                final SolveInfo match = this.engine.solve(new Struct(
                        "=",
                        currentBelief,
                        new Struct(
                                "movement",
                                new Var("S"),
                                new Var("D"))));
                if (match.isSuccess()) {
                    final Term speed = match.getTerm("S");
                    if (speed instanceof Number && getNode().getNodeSpeed() != ((Number) speed).doubleValue()) {
                        getNode().changeNodeSpeed(((Number) speed).doubleValue());
                    }

                    final Term direction = match.getTerm("D");
                    if (direction instanceof Number && getNode().getNodeDirectionAngle() != ((Number) direction).doubleValue()) {
                        getNode().changeDirectionAngle(((Number) direction).doubleValue(), false);
                    }
                }

                if (insertBelief.hasOpenAlternatives()) {
                    insertBelief = this.engine.solve(new Struct(
                            "retract",
                            new Struct(
                                    "added_belief",
                                    new Var("B"))));
                } else {
                    insertBelief = null;
                }
            }
        } catch (NoSolutionException e) {
            throw new IllegalStateException(this.getAgentName() + SEPARATOR + NO_SOLUTION_MSG + " addBelief/removeBelief.");
//            System.err.println(this.getAgentName() + SEPARATOR + NO_SOLUTION_MSG + " addBelief/removeBelief.");
        } catch (UnknownVarException e) {
            throw new IllegalStateException(this.getAgentName() + SEPARATOR + UNKNOWN_VAR_MSG + " addBelief/removeBelief.");
//            System.err.println(this.getAgentName() + SEPARATOR + UNKNOWN_VAR_MSG + " addBelief/removeBelief.");
        } catch (NoMoreSolutionException e) {
            throw new IllegalStateException(this.getAgentName() + SEPARATOR + NO_MORE_SOLUTION_MSG + " addBelief/removeBelief.");
//            System.err.println(this.getAgentName() + SEPARATOR + NO_MORE_SOLUTION_MSG + " addBelief/removeBelief.");
        }
    }

    /**
     * Triggers events in the agent for changes to the belief base.
     */
    protected void notifyBeliefBaseChanges() {
        // For each copule of the map trigger plan 'onAddBelief(term)', 'onRemoveBelief(term)' or 'onResponseMessage(term)' depending by the value of str
        this.beliefBaseChanges.forEach((term, str) -> {
            switch (str) {
                case ADD_NOTIFICATION:
                    final SolveInfo onAdd = this.engine.solve(new Struct("onAddBelief", term));
                    if (onAdd.isSuccess()) {
                        // System.out.println(this.getAgentName() + SEPARATOR + TRIGGERED_PLAN + " onAddBelief(" + term + ").");
                    } else {
                        System.err.println(this.getAgentName() + SEPARATOR + NO_IMPLEMENTATION_FOUND + " onAddBelief(" + term + ").");
                    }
                    break;
                case REMOVE_NOTIFICATION:
                    final SolveInfo onRemove = this.engine.solve(new Struct("onRemoveBelief", term));
                    if (onRemove.isSuccess()) {
                        // System.out.println(this.getAgentName() + SEPARATOR + TRIGGERED_PLAN + " onRemoveBelief(" + term + ").");
                    } else {
                        System.err.println(this.getAgentName() + SEPARATOR + NO_IMPLEMENTATION_FOUND + " onRemoveBelief(" + term + ").");
                    }
                    break;
                case RESPONSE_NOTIFICATION:
                    final SolveInfo onMessage = this.engine.solve(new Struct("onResponseMessage", term));
                    if (onMessage.isSuccess()) {
                        System.out.println(this.getAgentName() + SEPARATOR + TRIGGERED_PLAN + " onResponseMessage(" + term + ").");
                    } else {
                        System.err.println(this.getAgentName() + SEPARATOR + NO_IMPLEMENTATION_FOUND + " onResponseMessage(" + term + ").");
                    }
                    break;
            }
        });
        this.beliefBaseChanges.clear();
    }

    /**
     * Updates node location, notifies update and then overwrites distances from other agents.
     */
    protected void positionUpdate() {
        getNode().changeNodePosition(this.agentReaction.getTau());

        // Updates the belief 'position' in the agent
        final SolveInfo oldPosition = this.engine.solve(new Struct(
                "retract",
                new Struct(
                        "belief",
                        new Struct(
                                "position",
                                new Var("X"),
                                new Var("Y")))));
        if (oldPosition.isSuccess()) {
            final Position nodePosition = getNode().getNodePosition();
            final Struct positionBelief = new Struct(
                    "position",
                        new Double(nodePosition.getCoordinate(0)),
                        new Double(nodePosition.getCoordinate(1)));
            final Struct newPosition = new Struct("belief", positionBelief);

            this.engine.getTheoryManager().assertA(newPosition, true, null, false);
            // Adds the position updating as a add belief notification
            this.beliefBaseChanges.put(positionBelief, ADD_NOTIFICATION);

            // Updates beliefs with the distance to other agents
            this.updateAgentsDistances();
        } else {
            System.err.println(this.getAgentName() + SEPARATOR + NO_IMPLEMENTATION_FOUND + " position belief.");
        }
    }

    /**
     * Allows the agent to interact with a tuple space using the 'write', 'read', 'take' actions.
     */
    protected void retrieveTuples() {
        this.tripleListForTuples.forEach(tt -> {
            try {
                // Tries to solve the goal retracting the beliefs of the tuples
                SolveInfo solvedAction = this.engine.solve(tt.getFirst());
                while (solvedAction != null && solvedAction.isSuccess()) {
                    if (tt.getSecond().equals("write")) { // if the action is write the request is sent to the nearest blackboard
                        // Retrieves the nearest blackboard instance.
                        final AbstractAgent blackboard = getNode().getNearestBlackboard();
                        if (Objects.nonNull(blackboard) && blackboard.getClass().equals(Blackboard.class)) {
                            ((Blackboard) blackboard).insertRequest(solvedAction.getTerm("T"), this, tt.getSecond());
                        }
                    } else { // otherwise, for read and take actions the requests is sent to the neighborhood blackboard
                        // Retrieves the blackboard neighborhood instances.
                        final List<Blackboard> blackboardNeighborhood = getNode().getBlackboardNeighborhood();
                        for (Blackboard blackboard: blackboardNeighborhood) {
                            blackboard.insertRequest(solvedAction.getTerm("T"), this, tt.getSecond());
                        }
                    }

                    if (solvedAction.hasOpenAlternatives()) {
                        solvedAction = this.engine.solveNext();
                    } else {
                        solvedAction = null;
                    }
                }
            } catch (NoSolutionException e) {
                throw new IllegalStateException(this.getAgentName() + SEPARATOR + NO_SOLUTION_MSG + " " + tt.getThird() + ".");
//            System.err.println(this.getAgentName() + SEPARATOR + NO_SOLUTION_MSG + " " + tt.getThird() + ".");
            } catch (UnknownVarException e) {
                throw new IllegalStateException(this.getAgentName() + SEPARATOR + UNKNOWN_VAR_MSG + " " + tt.getThird() + ".");
//            System.err.println(this.getAgentName() + SEPARATOR + UNKNOWN_VAR_MSG + " " + tt.getThird() + ".");
            } catch (NoMoreSolutionException e) {
                throw new IllegalStateException(this.getAgentName() + SEPARATOR + NO_MORE_SOLUTION_MSG + " " + tt.getThird() + ".");
//            System.err.println(this.getAgentName() + SEPARATOR + NO_MORE_SOLUTION_MSG + " " + tt.getThird() + ".");
            }
        });
    }

    /**
     * Update agents distances
     */
    private void updateAgentsDistances() {
        try {
            // Retrieve the agent neighborhood distances
            final Map<String, java.lang.Double> newDistances = getNode().getNeighborhoodDistances();
            SolveInfo removeDitancesBeliefs = this.engine.solve(new Struct(
                    "retract",
                    new Struct(
                            "belief",
                            new Struct(
                                    "distance",
                                    new Var("A"),
                                    new Var("D")))));
            while (removeDitancesBeliefs != null && removeDitancesBeliefs.isSuccess()) {
                final String agentName = removeDitancesBeliefs.getTerm("A").toString();
                if (newDistances.keySet().contains(agentName)) { // if the agent of the distance just removed is still among the neighbors
                    // adds in the theory 'belief(distance(AGENT_NAME, NEW_DISTANCE)).'
                    final Struct distanceBelief = new Struct(
                            "belief",
                            new Struct(
                                    "distance",
                                    Term.createTerm(agentName),
                                    new Double(newDistances.get(agentName))));
                    this.engine.getTheoryManager().assertA(distanceBelief, true, null, false);

                    // adds in the belief notification 'distance(AGENT_NAME, NEW_DISTANCE, OLD_DISTANCE).'
                    final Struct distanceNotification = new Struct(
                            "distance",
                            Term.createTerm(agentName),
                            new Double(newDistances.get(agentName)),
                            removeDitancesBeliefs.getTerm("D"));
                    // Adds the position updating as a add belief notification
                    this.beliefBaseChanges.put(distanceNotification, ADD_NOTIFICATION);

                    newDistances.remove(agentName);
                }
                if (removeDitancesBeliefs.hasOpenAlternatives()) {
                    removeDitancesBeliefs = this.engine.solveNext();
                } else {
                    removeDitancesBeliefs = null;
                }
            }

            // adds remaining distances
            if (newDistances.size() > 0) {
                newDistances.forEach((strAgentName, distance) -> {
                    // adds in the theory 'belief(distance(AGENT_NAME, NEW_DISTANCE)).'
                    final Struct distanceBelief = new Struct(
                            "distance",
                            Term.createTerm(strAgentName),
                            new Double(distance));
                    final Struct newDistance = new Struct("belief", distanceBelief);
                    System.out.println(getAgentName() + " has new agent in the neighborhood: " + strAgentName);
                    this.engine.getTheoryManager().assertA(newDistance, true, null, false);
                    // Adds the position updating as a add belief notification
                    this.beliefBaseChanges.put(distanceBelief, ADD_NOTIFICATION);
                });
            }
        } catch (NoSolutionException e) {
            throw new IllegalStateException(this.getAgentName() + SEPARATOR + NO_SOLUTION_MSG + " to update agents distances.");
//            System.err.println(this.getAgentName() + SEPARATOR + NO_SOLUTION_MSG + " to update agents distances.");
        } catch (UnknownVarException e) {
            throw new IllegalStateException(this.getAgentName() + SEPARATOR + UNKNOWN_VAR_MSG + " to update agents distances.");
//            System.err.println(this.getAgentName() + SEPARATOR + UNKNOWN_VAR_MSG + " to update agents distances.");
        } catch (NoMoreSolutionException e) {
            throw new IllegalStateException(this.getAgentName() + SEPARATOR + NO_MORE_SOLUTION_MSG + " to update agents distances.");
//            System.err.println(this.getAgentName() + SEPARATOR + NO_MORE_SOLUTION_MSG + " to update agents distances.");
        }
    }

    /**
     * Add event response to the belief base changes.
     * @param response the event to notify.
     */
    protected void addResponseMessage(final Term response) {
        this.beliefBaseChanges.put(response, RESPONSE_NOTIFICATION);
    }

    //------------------------------------------
    // Agent's external action
    //------------------------------------------

    /**
     * Takes outgoing messages to send, clears the outbox and then returns messages.
     * @return List of outgoing messages.
     */
    public List<OutMessage> consumeOutgoingMessages() {
        final List<OutMessage> outMessages = new ArrayList<>(this.outbox);
        this.outbox.clear();
        return outMessages;
    }

    /**
     * Add incoming message to the in mailbox.
     * @param incomingMessage message to add.
     */
    public void addIncomingMessage(final OutMessage incomingMessage) {
        this.inbox.add(new InMessage(incomingMessage.getSender(), incomingMessage.getPayload()));
    }

    //------------------------------------------
    // Class of messages
    //------------------------------------------

    /**
     * Defines an incoming message.
     */
    private final class InMessage {
        private final Term sender;
        private final Term payload;

        /**
         * Constructor for incoming messages.
         * @param sender sender of message.
         * @param payload content of message.
         */
        private InMessage(final Term sender, final Term payload) {
            this.sender = sender;
            this.payload = payload;
        }

        private Term getSender() {
            return this.sender;
        }

        private Term getPayload() {
            return this.payload;
        }
    }

    /**
     * Defines an outgoing message.
     */
    public final class OutMessage {
        private final Term sender;
        private final Term receiver;
        private final Term payload;

        /**
         * Constructor for outgoing messages.
         * @param sender sender of message.
         * @param receiver receiver of message.
         * @param payload content of message.
         */
        private OutMessage(final Term sender, final Term receiver, final Term payload) {
            this.sender = sender;
            this.receiver = receiver;
            this.payload = payload;
        }

        public Term getSender() {
            return this.sender;
        }

        public Term getReceiver() {
            return this.receiver;
        }

        public Term getPayload() {
            return this.payload;
        }
    }
}
