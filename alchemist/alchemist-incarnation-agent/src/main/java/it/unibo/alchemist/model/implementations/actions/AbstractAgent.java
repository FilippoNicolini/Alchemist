package it.unibo.alchemist.model.implementations.actions;

import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.Library;
import alice.tuprolog.Long;
import alice.tuprolog.NoMoreSolutionException;
import alice.tuprolog.NoSolutionException;
import alice.tuprolog.Prolog;
import alice.tuprolog.SolveInfo;
import alice.tuprolog.Struct;
import alice.tuprolog.Term;
import alice.tuprolog.Theory;
import alice.tuprolog.UnknownVarException;
import alice.tuprolog.Var;
import alice.tuprolog.lib.InvalidObjectIdException;
import alice.tuprolog.lib.OOLibrary;
import it.unibo.alchemist.model.implementations.nodes.AgentsContainerNode;
import it.unibo.alchemist.model.interfaces.Context;
import it.unibo.alchemist.model.interfaces.Node;
import it.unibo.alchemist.model.interfaces.Reaction;
import kotlin.Triple;
import org.apache.commons.math3.distribution.LevyDistribution;
import org.apache.commons.math3.random.RandomGenerator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
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
//    private final String ADD_NOTIFICATION = "add";
//    private final String REMOVE_NOTIFICATION = "remove";
//    private final String RESPONSE_NOTIFICATION = "response";

    private final String agentName; // String for the agent name
    private boolean isInitialized = false; // Flag for init of the agent
    private final Queue<InMessage> inbox = new LinkedList<>(); // Mailbox IN queue
    private final Queue<OutMessage> outbox = new LinkedList<>(); // Mailbox OUT queue
    private final Prolog engine = new Prolog(); // tuProlog engine
    private final Library lib = this.engine.getLibrary("alice.tuprolog.lib.OOLibrary");
    private Reaction<Object> agentReaction; // Reference to reaction
    private RandomGenerator agentRandomGenerator; // Random generator of the reaction
    private LevyDistribution levyDistribution; // Distribution to be used with the random generator
//    private final Map<Term, String> beliefBaseChanges = new LinkedHashMap<>(); // Map where to save updated belief notifications
    private Queue<Term> intentionsStack = new LinkedList<>();

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
    private void setInitialized() {
        this.isInitialized = true;
    }

    /**
     * Get flag initialized.
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
     * Get the status of the intentionsStack.
     * @return true if the stack is empty
     */
    private boolean intentionsStakcIsEmpty() {
        return this.intentionsStack.size() == 0;
    }

    /**
     * Get the random generator of the agent.
     * @return random generator
     */
    protected RandomGenerator getAgentRandomGenerator() {
        return this.agentRandomGenerator;
    }

    /**
     * Get the reaction of the agent.
     * @return reaction
     */
    protected Reaction<Object> getAgentReaction() {
        return this.agentReaction;
    }

    @Override
    public AgentsContainerNode getNode() {
        return (AgentsContainerNode) super.getNode();
    }

    @Override
    public Context getContext() {
        return Context.NEIGHBORHOOD; // TODO va bene come profondità?
    }

    //*********************************************//
    //**         Agent's internal action         **//
    //*********************************************//

    /**
     * Load into the agent the theory containing the basic predicates and add also JavaObjectReference in tuProlog.
     */
    private void loadAgentLibrary() {
        try {
            this.engine.setTheory(new Theory(new FileInputStream(new File("alchemist-incarnation-agent/src/main/resources/" + BASE_THEORY + ".pl"))));
        } catch (IOException e) {
            throw new IllegalArgumentException(this.getAgentName() + SEPARATOR + IO_MSG);
        } catch (InvalidTheoryException e) {
            throw new IllegalArgumentException(this.getAgentName() + SEPARATOR + INVALID_THEORY_MSG);
        }

        try {
            this.levyDistribution = new LevyDistribution(this.agentRandomGenerator, 0, 0.5);
            ((OOLibrary) this.lib).register(new Struct("agent"), this); // object reference for internal actions
            ((OOLibrary) this.lib).register(new Struct("node"), this.getNode()); // object reference for external actions
        } catch (InvalidObjectIdException e) {
            throw new IllegalArgumentException(this.getAgentName() + SEPARATOR + INVALID_OBJECT_MSG);
        }
    }

    /**
     * Initilizes the agent (puts in the theory his name).
     */
    protected void initializeAgent() {
        // Name of the agent
        final Struct self = new Struct("self", Term.createTerm(this.getAgentName()));

        // Sets 'self' in the theory of the agent
        this.engine.getTheoryManager().assertA(self, true, null, false);

        // Sets the beliefs with the distance to other agents
//        this.updateAgentsDistances(); // TODO verificare se serve o eliminare e creare funzioni nel nodo

        // Prepares list to retrieve tuples
        this.tripleListForTuples.add(new Triple<>(
                new Struct("retract",
                    new Struct("write", new Var("T"))), "write", "writeTuple"));
        this.tripleListForTuples.add(new Triple<>(
                new Struct("retract",
                    new Struct("read",new Var("T"))), "read", "readTuple"));
        this.tripleListForTuples.add(new Triple<>(
                new Struct("retract",
                    new Struct("take",new Var("T"))), "take", "takeTuple"));

        // Updates the initialization flag
        this.setInitialized();
    }

    /**
     * Encapsulates the solution of the initialization plan for the agent.
     */
    protected void initReasoning() {
        // Solves init plan if present
        final SolveInfo init = this.getEngine().solve(new Struct("init"));
        if (!init.isSuccess()) {
            System.err.println(this.getAgentName() + SEPARATOR + NO_IMPLEMENTATION_FOUND + " init.");
        }
//        else {
//            // System.out.println(this.getAgentName() + SEPARATOR + "init " + SUCCESS_PLAN);
//        }
    }

    /**
     * Create an intention to handle the message read.
     */
    protected void readMessage() {
        if (!this.inboxIsEmpty()) {
            final InMessage msg = this.inbox.poll();
            if (Objects.nonNull(msg)) {
                // search the predicate in the theory
                final Struct receivedMessage = new Struct("onReceivedMessage", msg.getSender(), msg.getPayload());
                final SolveInfo checkClause = this.engine.solve(new Struct("clause", receivedMessage, new Var("Body")));
                if (checkClause.isSuccess()) {
//                    System.out.println("LOG" + SEPARATOR + getAgentName() + SEPARATOR + "leggo messaggio");
                    this.createIntention(checkClause, " onReceivedMessage.");
                } else {
                    System.err.println(this.getAgentName() + SEPARATOR + NO_IMPLEMENTATION_FOUND + receivedMessage);
                }
            }
        }
    }

    /**
     * Add a message to the outbox to be sended.
     * @param sender message sender
     * @param receiver message receiver
     * @param message message content
     */
    public void sendMessage(final Term sender, final Term receiver, final Term message) {
//        System.out.println("LOG" + SEPARATOR + getAgentName() + SEPARATOR + "invio messaggio");
        this.outbox.add(new OutMessage(sender, receiver, message));
    }

    /**
     * Recover added or removed beliefs and then trigger the related plan.
     */
    protected void beliefBaseChanges() {
        try {
            // Gets removed beliefs
            final Struct removedBeliefPredicate = new Struct("retract",
                    new Struct("removed_belief", new Var("B")));
            SolveInfo solveRemovedBelief = this.engine.solve(removedBeliefPredicate);
            while (solveRemovedBelief != null && solveRemovedBelief.isSuccess()) {
                // search the predicate in the theory
                final Struct removedBeliefNotify = new Struct("onRemoveBelief", solveRemovedBelief.getTerm("B"));
                final SolveInfo checkClause = this.engine.solve(new Struct("clause", removedBeliefNotify, new Var("Body")));
                if (checkClause.isSuccess()) {
                    this.createIntention(checkClause, " addBelief/removeBelief.");
                } else {
                    System.err.println(getAgentName() + SEPARATOR + NO_IMPLEMENTATION_FOUND + removedBeliefNotify);
                }
                if (solveRemovedBelief.hasOpenAlternatives()) {
                    solveRemovedBelief = this.engine.solve(removedBeliefPredicate);
                } else {
                    solveRemovedBelief = null;
                }
            }

            // Gets added beliefs
            final Struct addedBeliefPredicate = new Struct("retract",
                    new Struct("added_belief", new Var("B")));
            SolveInfo solveAddedBelief = this.engine.solve(addedBeliefPredicate);
            while (solveAddedBelief != null && solveAddedBelief.isSuccess()) {
                // search the predicate in the theory
                final Struct addedBeliefNotify = new Struct("onAddBelief", solveAddedBelief.getTerm("B"));
                final SolveInfo checkClause = this.engine.solve(new Struct("clause", addedBeliefNotify, new Var("Body")));
                if (checkClause.isSuccess()) {
                    this.createIntention(checkClause, " addBelief/removeBelief.");
                } else {
                    System.err.println(getAgentName() + SEPARATOR + NO_IMPLEMENTATION_FOUND + addedBeliefNotify);
                }
                if (solveAddedBelief.hasOpenAlternatives()) {
                    solveAddedBelief = this.engine.solve(addedBeliefPredicate);
                } else {
                    solveAddedBelief = null;
                }
            }
        } catch (NoSolutionException e) {
            throw new IllegalStateException(this.getAgentName() + SEPARATOR + NO_SOLUTION_MSG + " addBelief/removeBelief.");
        } catch (UnknownVarException e) {
            throw new IllegalStateException(this.getAgentName() + SEPARATOR + UNKNOWN_VAR_MSG + " addBelief/removeBelief.");
        }
    }

    /**
     * Internal action used to create and add the intention to the agent.
     * @param checkClause clause containing the body of the plan
     * @param errorMessage string to concat to the exception message
     */
    private void createIntention(final SolveInfo checkClause, final String errorMessage) {
        try {
            final SolveInfo unfoldBody = this.engine.solve(new Struct("unfold", checkClause.getTerm("Body"), new Var("BodyList")));
            // random id for the intention
            final Term id = new Long(new Date().getTime()); // TODO verificare univocità (concorrenza)
            // add id to the stack
            this.intentionsStack.add(id);
            // add intention to the theory
            final Struct intention = new Struct("intention", id, unfoldBody.getTerm("BodyList"));
            System.out.println("LOG" + SEPARATOR + getAgentName() + SEPARATOR + "add intention " + intention);
            this.engine.getTheoryManager().assertA(intention, true, null, false);
        } catch (NoSolutionException e) {
            throw new IllegalStateException(this.getAgentName() + SEPARATOR + NO_SOLUTION_MSG + errorMessage);
        } catch (UnknownVarException e) {
            throw new IllegalStateException(this.getAgentName() + SEPARATOR + UNKNOWN_VAR_MSG + errorMessage);
        }
    }

//    /**
//     * Update agents distances.
//     */
//    // TODO eliminare o modificare?
//    private void updateAgentsDistances() {
//        try {
//            // Retrieve the agent neighborhood distances
//            final Map<String, java.lang.Double> newDistances = getNode().getNeighborhoodDistances();
//            SolveInfo removeDitancesBeliefs = this.engine.solve(new Struct(
//                    "retract",
//                    new Struct(
//                            "belief",
//                            new Struct(
//                                    "distance",
//                                    new Var("A"),
//                                    new Var("D")))));
//            while (removeDitancesBeliefs != null && removeDitancesBeliefs.isSuccess()) {
//                final String agentName = removeDitancesBeliefs.getTerm("A").toString();
//                if (newDistances.keySet().contains(agentName)) { // if the agent of the distance just removed is still among the neighbors
//                    // adds in the theory 'belief(distance(AGENT_NAME, NEW_DISTANCE)).'
//                    final Struct distanceBelief = new Struct(
//                            "belief",
//                            new Struct(
//                                    "distance",
//                                    Term.createTerm(agentName),
//                                    new Double(newDistances.get(agentName))));
//                    this.engine.getTheoryManager().assertA(distanceBelief, true, null, false);
//
//                    // adds in the belief notification 'distance(AGENT_NAME, NEW_DISTANCE, OLD_DISTANCE).'
//                    final Struct distanceNotification = new Struct(
//                            "distance",
//                            Term.createTerm(agentName),
//                            new Double(newDistances.get(agentName)),
//                            removeDitancesBeliefs.getTerm("D"));
//                    // Adds the position updating as a add belief notification
//                    this.beliefBaseChanges.put(distanceNotification, ADD_NOTIFICATION);
//
//                    newDistances.remove(agentName);
//                }
//                if (removeDitancesBeliefs.hasOpenAlternatives()) {
//                    removeDitancesBeliefs = this.engine.solveNext();
//                } else {
//                    removeDitancesBeliefs = null;
//                }
//            }
//
//            // adds remaining distances
//            if (newDistances.size() > 0) {
//                newDistances.forEach((strAgentName, distance) -> {
//                    // adds in the theory 'belief(distance(AGENT_NAME, NEW_DISTANCE)).'
//                    final Struct distanceBelief = new Struct(
//                            "distance",
//                            Term.createTerm(strAgentName),
//                            new Double(distance));
//                    final Struct newDistance = new Struct("belief", distanceBelief);
//                    System.out.println(getAgentName() + " has new agent in the neighborhood: " + strAgentName);
//                    this.engine.getTheoryManager().assertA(newDistance, true, null, false);
//                    // Adds the position updating as a add belief notification
//                    this.beliefBaseChanges.put(distanceBelief, ADD_NOTIFICATION);
//                });
//            }
//        } catch (NoSolutionException e) {
//            throw new IllegalStateException(this.getAgentName() + SEPARATOR + NO_SOLUTION_MSG + " to update agents distances.");
//        } catch (UnknownVarException e) {
//            throw new IllegalStateException(this.getAgentName() + SEPARATOR + UNKNOWN_VAR_MSG + " to update agents distances.");
//        } catch (NoMoreSolutionException e) {
//            throw new IllegalStateException(this.getAgentName() + SEPARATOR + NO_MORE_SOLUTION_MSG + " to update agents distances.");
//        }
//    }

    /**
     * Choose the intention and then executes it.
     */
    protected void executeIntention() {
        if (!this.intentionsStakcIsEmpty()) {
            final Term intentionID = this.intentionsStack.poll();
            final Struct execIntention = new Struct("execute", intentionID);
            final SolveInfo solveExecIntention = this.engine.solve(execIntention);
            if (!solveExecIntention.isSuccess()) {
                System.err.println(this.getAgentName() + SEPARATOR + "Error executing intention " + execIntention);
            }
//            else {
//                System.out.println(this.getAgentName() + SEPARATOR + "Executed intention " + execIntention);
//            }
        }
    }

    /**
     * Remove a completed intention from the stack.
     * @param intentionID identifier of the intention to remove
     */
    public void removeCompletedIntention(final Term intentionID) {
        if (this.intentionsStack.contains(intentionID)) {
            this.intentionsStack.remove(intentionID);
            final Struct removeCompletedIntention = new Struct("intention", intentionID, new Var("X"));
            final SolveInfo solveRemovedCompletedIntention = this.engine.solve(removeCompletedIntention);
            if (solveRemovedCompletedIntention.isSuccess()) {
                System.out.println(this.getAgentName() + SEPARATOR + "Intention removed from the stack " + intentionID);
            } else {
                System.err.println(this.getAgentName() + SEPARATOR + "Failed removing intention from the stack " + intentionID);
            }
        } else {
            System.err.println(this.getAgentName() + SEPARATOR + "Agent not contains intention " + intentionID);
        }
    }

    public void executeInternalAction(final String action) {
        System.out.println("executeInternalAction: " + action);
        try {
            SolveInfo match = this.engine.solve(new Struct("=", Term.createTerm(action), new Struct("iSend", new Var("R"), new Var("M"))));
            if (match.isSuccess()) {
                this.sendMessage(Term.createTerm(this.getAgentName()), match.getTerm("R"), match.getTerm("M"));
            } else {
                // TODO aggiungere altre eventuali azioni iterne
                System.err.println("internal action not recognized for agent " + this.getAgentName());
            }
        } catch (NoSolutionException e) {
            throw new IllegalStateException(this.getAgentName() + SEPARATOR + NO_SOLUTION_MSG + " exec internal action " + action);
        } catch (UnknownVarException e) {
            throw new IllegalStateException(this.getAgentName() + SEPARATOR + UNKNOWN_VAR_MSG + " exec internal action " + action);
        }
    }

    public void test(final String msg) {
        System.out.println("test: " + msg);
    }

    //*********************************************//
    //**        Spatial Tuples Extension         **//
    //*********************************************//

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
            } catch (UnknownVarException e) {
                throw new IllegalStateException(this.getAgentName() + SEPARATOR + UNKNOWN_VAR_MSG + " " + tt.getThird() + ".");
            } catch (NoMoreSolutionException e) {
                throw new IllegalStateException(this.getAgentName() + SEPARATOR + NO_MORE_SOLUTION_MSG + " " + tt.getThird() + ".");
            }
        });
    }

    /**
     * Add event response to the belief base changes.
     * @param response the event to notify.
     */
    protected void addResponseMessage(final Term response) {
        // search the predicate in the theory
        final Struct responseMessageNotify = new Struct("onResponseMessage", response);
        final SolveInfo checkClause = this.engine.solve(new Struct("clause", responseMessageNotify, new Var("Body")));
        if (checkClause.isSuccess()) {
            this.createIntention(checkClause, " onResponseMessage.");
        } else {
            System.err.println(getAgentName() + SEPARATOR + NO_IMPLEMENTATION_FOUND + responseMessageNotify);
        }
    }

    //*********************************************//
    //**         Agent's external action         **//
    //*********************************************//

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

    //*********************************************//
    //**            Class of messages            **//
    //*********************************************//

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
