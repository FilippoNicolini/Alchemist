package it.unibo.alchemist.model.implementations.actions;

import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.MalformedGoalException;
import alice.tuprolog.NoMoreSolutionException;
import alice.tuprolog.NoSolutionException;
import alice.tuprolog.Prolog;
import alice.tuprolog.SolveInfo;
import alice.tuprolog.Struct;
import alice.tuprolog.Term;
import alice.tuprolog.Theory;
import alice.tuprolog.UnknownVarException;
import it.unibo.alchemist.model.implementations.nodes.AgentsContainerNode;
import it.unibo.alchemist.model.interfaces.Context;
import it.unibo.alchemist.model.interfaces.Node;
import it.unibo.alchemist.model.interfaces.Position;
import it.unibo.alchemist.model.interfaces.Reaction;
import kotlin.Triple;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
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

    // Strings used in the code
    private final List<Triple<String, String, String>> tripleListForTuples = new ArrayList<>();
    private final static String ADD_NOTIFICATION = "add";
    private final static String REMOVE_NOTIFICATION = "remove";
    private final static String RESPONSE_NOTIFICATION = "response";

    private final String agentName; // String for the agent name
    private boolean isInitialized = false; // Flag for init of the agent
    private final Queue<InMessage> inbox = new LinkedList<>(); // Mailbox IN queue
    private final Queue<OutMessage> outbox = new LinkedList<>(); // Mailbox OUT queue
    private final Prolog engine = new Prolog(); // tuProlog engine
    private Reaction agentReaction; // Reference to reaction
    private final Map<Term, String> beliefBaseChanges = new LinkedHashMap<>();

    /**
     * Constructor for abstract agent.
     * @param name agent name.
     * @param node node where the agent is placed.
     */
    protected AbstractAgent(final String name, final Node<Object> node) {
        super(node);
        this.agentName = name;

        try {
            this.engine.setTheory(new Theory(new FileInputStream(new File("alchemist-incarnation-agent/src/main/resources/" + BASE_THEORY + ".pl"))));
        } catch (IOException e) {
            System.err.println(this.getAgentName() + SEPARATOR + IO_MSG);
        } catch (InvalidTheoryException e) {
            System.err.println(this.getAgentName() + SEPARATOR + INVALID_THEORY_MSG);
        }
    }

    /**
     * Constructor for abstract agent.
     * @param name agent name.
     * @param node node where the agent is placed.
     * @param reaction reaction of the agent.
     */
    protected AbstractAgent(final String name, final Node<Object> node, final Reaction<Object> reaction) {
        super(node);
        this.agentName = name;
        this.agentReaction = reaction;

        try {
            this.engine.setTheory(new Theory(new FileInputStream(new File("alchemist-incarnation-agent/src/main/resources/" + BASE_THEORY + ".pl"))));
        } catch (IOException e) {
            System.err.println(this.getAgentName() + SEPARATOR + IO_MSG);
        } catch (InvalidTheoryException e) {
            System.err.println(this.getAgentName() + SEPARATOR + INVALID_THEORY_MSG);
        }
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

    @Override
    public Context getContext() {
        return Context.NEIGHBORHOOD; // TODO va bene come profondità?
    }

    //------------------------------------------
    // Agent's internal action
    //------------------------------------------

    /**
     * Initilizes the agent (puts in the theory his name).
     */
    protected void inizializeAgent() {
        // Name of the agent
        final Struct self = new Struct("self", Term.createTerm(this.getAgentName()));

        // Sets 'self' in the theory of the agent
        this.engine.getTheoryManager().assertA(self, true, null, false);

        // Node's starting position
        final Position nodePosition = ((AgentsContainerNode) getNode()).getNodePosition();
        final Struct position = new Struct(
                "belief",
                new Struct(
                        "position",
                        Term.createTerm(Double.toString(nodePosition.getCoordinate(0))),
                        Term.createTerm(Double.toString(nodePosition.getCoordinate(1))))
        );

        // Node's initial speed and direction
        final Struct movement = new Struct(
                "belief",
                new Struct(
                        "movement",
                        Term.createTerm(((AgentsContainerNode) getNode()).getNodeSpeed().toString()),
                        Term.createTerm(((AgentsContainerNode) getNode()).getNodeDirectionAngle().toString()))
        );

        // Sets the node position
        this.engine.getTheoryManager().assertA(position, true, null, false);

        // Sets the node movement
        this.engine.getTheoryManager().assertA(movement, true, null, false);

        // Sets the beliefs with the distance to other agents
        this.agentsDistancesUpdate();

        // Prepares list to retrieve tuples
        this.tripleListForTuples.add(new Triple<>("retract(write(BB,T)).", "write", "writeTuple"));
        this.tripleListForTuples.add(new Triple<>("retract(read(BB,T)).", "read", "readTuple"));
        this.tripleListForTuples.add(new Triple<>("retract(take(BB,T)).", "take", "takeTuple"));

        // Updates the initialization flag
        this.setInitialized();
    }

    /**
     * Encapsulates the solution of the initialization plan for the agent.
     */
    protected void firstReasoning() {
        try {
            // Solves init plan if present
            final SolveInfo init = this.getEngine().solve("init.");
            if (init.isSuccess()) {
                System.out.println(this.getAgentName() + SEPARATOR + "init " + SUCCESS_PLAN);
            } else {
                System.err.println(this.getAgentName() + SEPARATOR + NO_IMPLEMENTATION_FOUND + " init.");
            }
            this.hanldeOutGoingMessages();
        } catch (MalformedGoalException e) {
            System.err.println(this.getAgentName() + SEPARATOR + MALFORMED_GOAL_MSG + " init.");
        }
    }

    /**
     * Inserts messages received into the theory.
     */
    protected void handleIncomingMessages() {
        while (!this.inboxIsEmpty()) {
            final InMessage msg = this.inbox.poll();
            if (Objects.nonNull(msg)) {
                final Struct incoming = new Struct("ingoing", msg.getSender(), msg.getPayload());
                this.engine.getTheoryManager().assertZ(incoming, true, null, false);
            }
        }
    }

    /**
     * Prepares messages to send.
     */
    protected void hanldeOutGoingMessages() {
        try {
            // Retracts outgoing beliefs in the theory and adds them as messages to the outbox.
            SolveInfo outgoing = this.engine.solve("retract(outgoing(S,R,M)).");
            while (outgoing != null && outgoing.isSuccess()) {
                this.outbox.add(new OutMessage(outgoing.getTerm("S"), outgoing.getTerm("R"), outgoing.getTerm("M")));
                if (outgoing.hasOpenAlternatives()) {
                    outgoing = this.engine.solveNext();
                } else {
                    outgoing = null;
                }
            }
        } catch (MalformedGoalException e) {
            System.err.println(this.getAgentName() + SEPARATOR + MALFORMED_GOAL_MSG + " outgoing.");
        } catch (NoSolutionException e) {
            System.err.println(this.getAgentName() + SEPARATOR + NO_SOLUTION_MSG + " outgoing.");
        } catch (UnknownVarException e) {
            System.err.println(this.getAgentName() + SEPARATOR + UNKNOWN_VAR_MSG + " outgoing.");
        } catch (NoMoreSolutionException e) {
            System.err.println(this.getAgentName() + SEPARATOR + NO_MORE_SOLUTION_MSG + " outgoing.");
        }
    }

    /**
     * Solve receiveMessage plan.
     */
    protected void readMessage() {
        try {
            final SolveInfo receive = this.engine.solve("receiveMessage.");
            if (receive.isSuccess()) {
                System.out.println(this.getAgentName() + SEPARATOR + this.agentReaction.getTau().toDouble() + SEPARATOR + "receiveMessage " + SUCCESS_PLAN);
            }
        } catch (MalformedGoalException e) {
            System.err.println(this.getAgentName() + SEPARATOR + MALFORMED_GOAL_MSG + " receiveMessage.");
        }
    }

    /**
     * Takes changes to the belief base occurred in the reasoning cycle.
     */
    protected void getBeliefBaseChanges() {
        try {
            // Gets added beliefs
            try {
                SolveInfo insertBelief = this.engine.solve("retract(added_belief(B)).");
                while (insertBelief != null && insertBelief.isSuccess()) {
                    // Puts beliefs in the map for the notification
                    final Term currentBelief = insertBelief.getTerm("B");
                    this.beliefBaseChanges.put(currentBelief, ADD_NOTIFICATION);

                    // Updates node values of speed and direction (that are contained into the belief 'movement')
                    final JSONObject jsonObj = new JSONObject(currentBelief.toJSON());
                    if ("movement".equals(jsonObj.get("name"))) {
                        final JSONArray args = (JSONArray) jsonObj.get("arg");
                        // Speed
                        final String speed = args.getJSONObject(0).get("value").toString();
                        if (!((AgentsContainerNode) getNode()).getNodeSpeed().toString().equals(speed)) {
                            ((AgentsContainerNode) getNode()).changeNodeSpeed(Double.parseDouble(speed));
                        }

                        // Direction
                        final String direction = args.getJSONObject(1).get("value").toString();
                        if (!((AgentsContainerNode) getNode()).getNodeDirectionAngle().toString().equals(direction)) {
                            ((AgentsContainerNode) getNode()).changeDirectionAngle(Integer.parseInt(direction), false);
                        }
                    }

                    if (insertBelief.hasOpenAlternatives()) {
                        insertBelief = this.engine.solveNext();
                    } else {
                        insertBelief = null;
                    }
                }
            } catch (MalformedGoalException e) {
                System.err.println(this.getAgentName() + SEPARATOR + MALFORMED_GOAL_MSG + " added_belief.");
            }

            // Gets removed beliefs
            try {
                SolveInfo removeBelief = this.engine.solve("retract(removed_belief(B)).");
                while (removeBelief != null && removeBelief.isSuccess()) {
                    // Puts beliefs in the map for the notification
                    this.beliefBaseChanges.put(removeBelief.getTerm("B"), REMOVE_NOTIFICATION);
                    if (removeBelief.hasOpenAlternatives()) {
                        removeBelief = this.engine.solveNext();
                    } else {
                        removeBelief = null;
                    }
                }
            } catch (MalformedGoalException e) {
                System.err.println(this.getAgentName() + SEPARATOR + MALFORMED_GOAL_MSG + " removed_belief.");
            }
        } catch (NoSolutionException e) {
            System.err.println(this.getAgentName() + SEPARATOR + NO_SOLUTION_MSG + " addBelief/removeBelief.");
        } catch (UnknownVarException e) {
            System.err.println(this.getAgentName() + SEPARATOR + UNKNOWN_VAR_MSG + " addBelief/removeBelief.");
        } catch (NoMoreSolutionException e) {
            System.err.println(this.getAgentName() + SEPARATOR + NO_MORE_SOLUTION_MSG + " addBelief/removeBelief.");
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
                    try {
                        final SolveInfo onAdd = this.engine.solve("onAddBelief(" + term + ").");
                        if (onAdd.isSuccess()) {
                            System.out.println(this.getAgentName() + SEPARATOR + TRIGGERED_PLAN + " onAddBelief(" + term + ").");
                        } else {
                            System.err.println(this.getAgentName() + SEPARATOR + NO_IMPLEMENTATION_FOUND + " onAddBelief(" + term + ").");
                        }
                    } catch (MalformedGoalException e) {
                        System.err.println(this.getAgentName() + SEPARATOR + MALFORMED_GOAL_MSG + " onAddBelief.");
                    }
                    break;
                case REMOVE_NOTIFICATION:
                    try {
                        final SolveInfo onRemove = this.engine.solve("onRemoveBelief(" + term + ").");
                        if (onRemove.isSuccess()) {
                            System.out.println(this.getAgentName() + SEPARATOR + TRIGGERED_PLAN + " onRemoveBelief(" + term + ").");
                        } else {
                            System.err.println(this.getAgentName() + SEPARATOR + NO_IMPLEMENTATION_FOUND + " onRemoveBelief(" + term + ").");
                        }
                    } catch (MalformedGoalException e) {
                        System.err.println(this.getAgentName() + SEPARATOR + MALFORMED_GOAL_MSG + " onRemoveBelief.");
                    }
                    break;
                case RESPONSE_NOTIFICATION:
                    try {
                        final SolveInfo onMessage = this.engine.solve("onResponseMessage(" + term + ").");
                        if (onMessage.isSuccess()) {
                            System.out.println(this.getAgentName() + SEPARATOR + TRIGGERED_PLAN + " onResponseMessage(" + term + ").");
                        } else {
                            System.err.println(this.getAgentName() + SEPARATOR + NO_IMPLEMENTATION_FOUND + " onResponseMessage(" + term + ").");
                        }
                    } catch (MalformedGoalException e) {
                        System.err.println(this.getAgentName() + SEPARATOR + MALFORMED_GOAL_MSG + " onResponseMessage.");
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
        ((AgentsContainerNode) getNode()).changeNodePosition(this.agentReaction.getTau());

        // Updates the belief 'position' in the agent
        try {
            final SolveInfo oldPosition = this.engine.solve("retract(belief(position(X,Y))).");
            if (oldPosition.isSuccess()) {
                final Position nodePosition = ((AgentsContainerNode) getNode()).getNodePosition();
                final Struct positionBelief = new Struct(
                        "position",
                        Term.createTerm(Double.toString(nodePosition.getCoordinate(0))),
                        Term.createTerm(Double.toString(nodePosition.getCoordinate(1))));
                final Struct newPosition = new Struct("belief", positionBelief);

                this.engine.getTheoryManager().assertA(newPosition, true, null, false);
                // Adds the position updating as a add belief notification
                this.beliefBaseChanges.put(positionBelief, ADD_NOTIFICATION);

                // Updates beliefs with the distance to other agents
                this.agentsDistancesUpdate();
            } else {
                System.err.println(this.getAgentName() + SEPARATOR + NO_IMPLEMENTATION_FOUND + " position belief.");
            }
        } catch (MalformedGoalException e) {
            System.err.println(this.getAgentName() + SEPARATOR + MALFORMED_GOAL_MSG + " position belief.");
        }
    }

    /**
     * Allows the agent to interact with a tuple space using the 'write', 'read', 'take' actions.
     */
    protected void retrieveTuples() {
        this.tripleListForTuples.forEach(tt -> {
            try {
                // Tries to solve the goal retracting the beliefs
                SolveInfo solvedAction = this.engine.solve(tt.getFirst());
                while (solvedAction != null && solvedAction.isSuccess()) {
                    // Retrieves the blackboard instance using the agent name in which the request is placed.
                    final AbstractAgent blackboard = ((AgentsContainerNode) getNode()).getNeighborAgent(solvedAction.getTerm("BB").toString());
                    if (Objects.nonNull(blackboard) && blackboard.getClass().equals(Blackboard.class)) {
                        ((Blackboard) blackboard).insertRequest(solvedAction.getTerm("T").toString(), this, tt.getSecond());
                    }
                    if (solvedAction.hasOpenAlternatives()) {
                        solvedAction = this.engine.solveNext();
                    } else {
                        solvedAction = null;
                    }
                }
            } catch (MalformedGoalException e) {
                System.err.println(this.getAgentName() + SEPARATOR + MALFORMED_GOAL_MSG + " " + tt.getThird() + ".");
            } catch (NoMoreSolutionException e) {
                System.err.println(this.getAgentName() + SEPARATOR + NO_MORE_SOLUTION_MSG + " " + tt.getThird() + ".");
            } catch (NoSolutionException e) {
                System.err.println(this.getAgentName() + SEPARATOR + NO_SOLUTION_MSG + " " + tt.getThird() + ".");
            } catch (UnknownVarException e) {
                System.err.println(this.getAgentName() + SEPARATOR + UNKNOWN_VAR_MSG + " " + tt.getThird() + ".");
            }
        });
    }

//    protected void retrieveTuples() {
//        try {
//            SolveInfo writeAction = this.engine.solve("retract(write(BB,T)).");
//            while (writeAction != null && writeAction.isSuccess()) {
//                final AbstractAgent blackboard = ((AgentsContainerNode) getNode()).getNeighborAgent(writeAction.getTerm("BB").toString());
//                if (Objects.nonNull(blackboard) && blackboard.getClass().equals(Blackboard.class)) {
//                    ((Blackboard) blackboard).insertRequest(writeAction.getTerm("T").toString(), this, "write");
//                }
//                if (writeAction.hasOpenAlternatives()) {
//                    writeAction = this.engine.solveNext();
//                } else {
//                    writeAction = null;
//                }
//            }
//        } catch (MalformedGoalException e) {
//            System.err.println(this.getAgentName() + SEPARATOR + MALFORMED_GOAL_MSG + " writeTuple.");
//        } catch (NoMoreSolutionException e) {
//            System.err.println(this.getAgentName() + SEPARATOR + NO_MORE_SOLUTION_MSG + " writeTuple.");
//        } catch (NoSolutionException e) {
//            System.err.println(this.getAgentName() + SEPARATOR + NO_SOLUTION_MSG + " writeTuple.");
//        } catch (UnknownVarException e) {
//            System.err.println(this.getAgentName() + SEPARATOR + UNKNOWN_VAR_MSG + " writeTuple.");
//        }
//
//        try {
//            SolveInfo readAction = this.engine.solve("retract(read(BB,T)).");
//            while (readAction != null && readAction.isSuccess()) {
//                final AbstractAgent blackboard = ((AgentsContainerNode) getNode()).getNeighborAgent(readAction.getTerm("BB").toString());
//                if (Objects.nonNull(blackboard) && blackboard.getClass().equals(Blackboard.class)) {
//                    ((Blackboard) blackboard).insertRequest(readAction.getTerm("T").toString(), this, "read");
//                }
//                if (readAction.hasOpenAlternatives()) {
//                    readAction = this.engine.solveNext();
//                } else {
//                    readAction = null;
//                }
//            }
//        } catch (MalformedGoalException e) {
//            System.err.println(this.getAgentName() + SEPARATOR + MALFORMED_GOAL_MSG + " readTuple.");
//        } catch (NoMoreSolutionException e) {
//            System.err.println(this.getAgentName() + SEPARATOR + NO_MORE_SOLUTION_MSG + " readTuple.");
//        } catch (NoSolutionException e) {
//            System.err.println(this.getAgentName() + SEPARATOR + NO_SOLUTION_MSG + " readTuple.");
//        } catch (UnknownVarException e) {
//            System.err.println(this.getAgentName() + SEPARATOR + UNKNOWN_VAR_MSG + " readTuple.");
//        }
//
//        try {
//            SolveInfo takeAction = this.engine.solve("retract(take(BB,T)).");
//            while (takeAction != null && takeAction.isSuccess()) {
//                final AbstractAgent blackboard = ((AgentsContainerNode) getNode()).getNeighborAgent(takeAction.getTerm("BB").toString());
//                if (Objects.nonNull(blackboard) && blackboard.getClass().equals(Blackboard.class)) {
//                    ((Blackboard) blackboard).insertRequest(takeAction.getTerm("T").toString(), this, "take");
//                }
//                if (takeAction.hasOpenAlternatives()) {
//                    takeAction = this.engine.solveNext();
//                } else {
//                    takeAction = null;
//                }
//            }
//        } catch (MalformedGoalException e) {
//            System.err.println(this.getAgentName() + SEPARATOR + MALFORMED_GOAL_MSG + " takeTuple.");
//        } catch (NoMoreSolutionException e) {
//            System.err.println(this.getAgentName() + SEPARATOR + NO_MORE_SOLUTION_MSG + " takeTuple.");
//        } catch (NoSolutionException e) {
//            System.err.println(this.getAgentName() + SEPARATOR + NO_SOLUTION_MSG + " takeTuple.");
//        } catch (UnknownVarException e) {
//            System.err.println(this.getAgentName() + SEPARATOR + UNKNOWN_VAR_MSG + " takeTuple.");
//        }
//    }

    /**
     * Updates distance from agent's node to all other agents.
     */
    protected void agentsDistancesUpdate() {
        // Removes all previews distances
        try {
            SolveInfo removeDitancesBeliefs = this.engine.solve("retract(belief(distance(_,_))).");
            while (removeDitancesBeliefs != null && removeDitancesBeliefs.isSuccess()) {
                if (removeDitancesBeliefs.hasOpenAlternatives()) {
                    removeDitancesBeliefs = this.engine.solveNext();
                } else {
                    removeDitancesBeliefs = null;
                }
            }
        } catch (MalformedGoalException e) {
            System.err.println(this.getAgentName() + SEPARATOR + MALFORMED_GOAL_MSG + " to remove agents distances.");
        } catch (NoMoreSolutionException e) {
            System.err.println(this.getAgentName() + SEPARATOR + NO_MORE_SOLUTION_MSG + " to remove agents distances.");
        }

        // Inserts the distances updated and triggers notification
        ((AgentsContainerNode) getNode()).getNeighborhoodDistances().forEach((strAgentName, distance) -> {

            final Struct distanceBelief = new Struct(
                    "distance",
                    Term.createTerm(strAgentName),
                    Term.createTerm(distance.toString()));
            final Struct newDistance = new Struct("belief", distanceBelief);

            this.engine.getTheoryManager().assertA(newDistance, true, null, false);
            // Adds the position updating as a add belief notification
            this.beliefBaseChanges.put(distanceBelief, ADD_NOTIFICATION);
        });
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
