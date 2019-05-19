package it.unibo.alchemist.model.implementations.actions;

import alice.tuprolog.*;
import it.unibo.alchemist.model.interfaces.Node;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract definition of Spatial Tuples agents. Contains methods for managing the agent reasoning Cycle.
 */
public abstract class AbstractSpatialTuple extends AbstractAgent {

    private final List<Request> incomingRequests = new ArrayList<>();
    private final List<Request> pendingRequests = new ArrayList<>();

    /**
     * Constructor for abstract tuple space agent.
     * @param spatialTupleName agent name.
     * @param node node where the agent is placed.
     * @param rand random generator.
     */
    protected AbstractSpatialTuple(final String spatialTupleName, final Node<Object> node, final RandomGenerator rand) {
        super(spatialTupleName, node, rand);

    }

    //*********************************************//
    //**         Agent's internal action         **//
    //*********************************************//

    /**
     * Handles new requests received from the spatial tuple.
     */
    protected void handleIncomingRequests() {
        while (!(this.incomingRequests.size() == 0)) {
            final Request request = this.incomingRequests.remove(0);
            // System.out.println("REQUEST " + request.getAction() + " --> " + request.getTemplate());
            switch (request.getAction()) {
                case "write":
                    writeOnSpatialTuple(request);
                    break;
                case "read":
                    readFromSpatialTuple(request, false);
                    break;
                case "take":
                    takeFromSpatialTuple(request, false);
                    break;
            }
        }
    }

    /**
     * Iterates pending requests to try to solve them.
     */
    protected void handlePendingRequests() {
        for (int i = 0; i < this.pendingRequests.size(); i++) {
            final Request currRequest = this.pendingRequests.get(i);
            switch (currRequest.getAction()) {
                case "read":
                    readFromSpatialTuple(currRequest, true);
                    break;
                case "take":
                    takeFromSpatialTuple(currRequest, true);
                    break;
            }
        }
    }

    /**
     * Add a request to the pending queue.
     * @param request request to add.
     */
    protected void addToPendingQueue(final Request request) {
        this.pendingRequests.add(request);
    }

    /**
     * Remove a request from the pending queue.
     * @param request request to remove.
     */
    protected void removeFromPendingQueue(final Request request) {
        this.pendingRequests.remove(request);
    }

    /**
     * Insert a term into the theory.
     * @param request object that contains the term to write into the theory.
     */
    protected void writeOnSpatialTuple(final Request request) {
        final SolveInfo msgToWrite = this.getEngine().solve(new Struct("assertz", request.getTemplate()));
        if (msgToWrite.isSuccess()) {
            this.handlePendingRequests();
        } else {
            throw new IllegalArgumentException("Malformed template to write on tuple space");
        }
    }

    /**
     * Try to read a template from the theory. If it has success return the solution to the agent.
     * @param request object that contains the template to read from the theory.
     * @param isPending boolean that indicate if the request object is taken from pending queue.
     */
    protected void readFromSpatialTuple(final Request request, final boolean isPending) {
        try {
            final SolveInfo verifyMatch = this.getEngine().solve(request.getTemplate());
            if (verifyMatch.isSuccess()) {
                if (isPending) {
                    this.removeFromPendingQueue(request);
                }
                request.getAgent().addResponseMessage(new Struct(
                        "msg",
                        verifyMatch.getSolution()));
            } else {
                if (!isPending) {
                    // System.out.println("ADD TO PENDING || " + request.getAction() + " || " + request.getTemplate());
                    this.addToPendingQueue(request);
                }
            }
        } catch (NoSolutionException e) {
            throw new IllegalStateException(this.getAgentName() + SEPARATOR + NO_SOLUTION_MSG + " read on tuple space.");
        }
    }

    /**
     * Try to take a template from the theory. If it has success (a term is removed from the theory) return the solution to the agent.
     * @param request object that contains the template to read from the theory.
     * @param isPending boolean that indicate if the request object is taken from pending queue.
     */
    protected void takeFromSpatialTuple(final Request request, final boolean isPending) {
        try {
            final SolveInfo verifyMatch = this.getEngine().solve(new Struct("retract", request.getTemplate()));
            if (verifyMatch.isSuccess()) {
                if (isPending) {
                    this.removeFromPendingQueue(request);
                }
                try {
                    final SolveInfo match = this.getEngine().solve(new Struct(
                            "=",
                            verifyMatch.getSolution(),
                            new Struct(
                                    "retract",
                                    new Var("X"))));
                    if (match.isSuccess()) {
                        final Term term = match.getTerm("X");
                        request.getAgent().addResponseMessage(new Struct(
                                "msg",
                                term));
                    }
                } catch (UnknownVarException e) {
                    throw new IllegalStateException(this.getAgentName() + SEPARATOR + UNKNOWN_VAR_MSG + " take on tuple space.");
                }
            } else {
                if (!isPending) {
                    // System.out.println("ADD TO PENDING || " + request.getAction() + " || " + request.getTemplate());
                    this.addToPendingQueue(request);
                }
            }
        } catch (NoSolutionException e) {
            throw new IllegalStateException(this.getAgentName() + SEPARATOR + NO_SOLUTION_MSG + " take on tuple space.");
        }
    }


    //*********************************************//
    //**         Agent's external action         **//
    //*********************************************//

    /**
     * Gets values and creates a request to put in the incoming queue.
     * @param template template to find (case of 'read' or 'take') or term to insert (case of 'write').
     * @param agent agent instance.
     * @param action action to performe ('write', 'read', 'take').
     */
    public void insertRequest(final Term template, final AbstractAgent agent, final String action) {
        this.incomingRequests.add(new Request(template, agent, action));
    }

    /**
     * Defines a waiting agent.
     */
    public final class Request {
        private final Term template;
        private final AbstractAgent agent;
        private final String action;

        /**
         * Constructor for the request.
         * @param template template to find (case of 'read' or 'take') or term to insert (case of 'write').
         * @param agent agent instance.
         * @param action action to performe ('write', 'read', 'take').
         */
        private Request(final Term template, final AbstractAgent agent, final String action) {
            this.template = template;
            this.agent = agent;
            this.action = action;
        }

        /**
         * Get the term of the request.
         * @return term of the request.
         */
        public Term getTemplate() {
            return this.template;
        }

        /**
         * Get the agent who made the request.
         * @return agent instance.
         */
        public AbstractAgent getAgent() {
            return this.agent;
        }

        /**
         * Get the action of the request.
         * @return the string of the action.
         */
        public String getAction() {
            return this.action;
        }

    }
}
