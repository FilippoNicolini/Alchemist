package it.unibo.alchemist.model.implementations.actions;

import alice.tuprolog.MalformedGoalException;
import alice.tuprolog.NoSolutionException;
import alice.tuprolog.SolveInfo;
import alice.tuprolog.Term;
import alice.tuprolog.Var;
import it.unibo.alchemist.model.interfaces.Action;
import it.unibo.alchemist.model.interfaces.Node;
import it.unibo.alchemist.model.interfaces.Reaction;

import java.util.*;

/**
 * Implementation of a blackboard for tuple space extension.
 * Used by agents for 'write', 'read', 'take' messages.
 */
public class Blackboard extends AbstractAgent {

    private final List<Request> incomingRequests = new ArrayList<>();
    private final List<Request> pendingRequests = new ArrayList<>();

    /**
     * Constructor for blackboard.
     * @param blackboardName name of the blackboard.
     * @param node node where the agent is placed.
     */
    public Blackboard(final String blackboardName, final Node<Object> node) {
        super(blackboardName, node);
    }

    @Override
    public Action<Object> cloneAction(final Node<Object> n, final Reaction<Object> r) {
        return new Blackboard("cloned_" + this.getAgentName(), getNode());
    }

    @Override
    public void execute() {
        if (!this.isInitialized()) {
            this.inizializeAgent();

            System.out.println("Nodo: " + getNode().getId() + " || agent " + this.getAgentName() + " inizializzato");

            this.firstReasoning();
        } else {

            this.handleIncomingRequests();

            this.handlePendingRequests();
        }
    }


    //------------------------------------------
    // Agent's internal action
    //------------------------------------------

    /**
     * Handles new requests received from the blackboard.
     */
    private void handleIncomingRequests() {
        while (!(this.incomingRequests.size() == 0)) {
            final Request request = this.incomingRequests.remove(0);
            // System.out.println("REQUEST " + request.getAction() + " --> " + request.getTemplate());
            switch (request.getAction()) {
                case "write":
                    this.writeOnBlackboard(request);
                    break;
                case "read":
                    this.readFromBlackboard(request, false);
                    break;
                case "take":
                    this.takeFromBlackboard(request, false);
                    break;
            }
        }
    }

    /**
     * Iterates pending requests to try to solve them.
     */
    private void handlePendingRequests() {
        for (int i = 0; i < this.pendingRequests.size(); i++) {
            final Request currRequest = this.pendingRequests.get(i);
            switch (currRequest.getAction()) {
                case "read":
                    readFromBlackboard(currRequest, true);
                    break;
                case "take":
                    takeFromBlackboard(currRequest, true);
                    break;
            }

        }
    }

    /**
     * Write the request on the blackboard theory.
     * @param request the request to add.
     */
    private void writeOnBlackboard(final Request request) {
        try {
            final SolveInfo msgToWrite = this.getEngine().solve("assertz(" + request.getTemplate() + ").");
            if (msgToWrite.isSuccess()) {
                this.handlePendingRequests();
            } else {
                System.err.println("Malformed template to write on blackboard");
            }

        } catch (MalformedGoalException e) {
            System.err.println(this.getAgentName() + SEPARATOR + MALFORMED_GOAL_MSG + " to write on blackboard.");
        }
    }

    /**
     * Tries to read the request from blackboard theory. If has success return the solution to the agent.
     * @param request request to read.
     * @param isPending boolean to check if the request comes from the pending queue.
     */
    private void readFromBlackboard(final Request request, final boolean isPending) {
        try {
            final SolveInfo verifyMatch = this.getEngine().solve(request.getTemplate() + ".");
            if (verifyMatch.isSuccess()) {
                if (isPending) {
                    this.pendingRequests.remove(request);
                }
                request.getAgent().addResponseMessage(verifyMatch.getSolution());
            } else {
                if (!isPending) {
                    System.out.println("ADD TO PENDING || " + request.getAction() + " || " + request.getTemplate());
                    this.pendingRequests.add(request);
                }
            }
        } catch (MalformedGoalException e) {
            System.err.println(this.getAgentName() + SEPARATOR + MALFORMED_GOAL_MSG + " to read on blackboard.");
        } catch (NoSolutionException e) {
            System.err.println(this.getAgentName() + SEPARATOR + NO_SOLUTION_MSG + " to read on blackboard.");
        }
    }

    /**
     * Tries to take the request from blackboard theory. If has success remove the message from the theory and return the solution to the agent.
     * @param request request to take.
     * @param isPending boolean to check if the request comes from the pending queue.
     */
    private void takeFromBlackboard(final Request request, final boolean isPending) {
        try {
            final SolveInfo verifyMatch = this.getEngine().solve("retract(" + request.getTemplate() + ").");
            if (verifyMatch.isSuccess()) {
                if (isPending) {
                    this.pendingRequests.remove(request);
                }
                // Replaces the binding vars with term in the solution
                String response = request.getTemplate();
                for (int i = 0; i < verifyMatch.getBindingVars().size(); i++) {
                    final Var currVar = verifyMatch.getBindingVars().get(i);
                    response = response.replace(currVar.getName(), currVar.getTerm().toString());
                }
                request.getAgent().addResponseMessage(Term.createTerm(response));
            } else {
                if (!isPending) {
                    System.out.println("ADD TO PENDING || " + request.getAction() + " || " + request.getTemplate());
                    this.pendingRequests.add(request);
                }
            }
        } catch (MalformedGoalException e) {
            System.err.println(this.getAgentName() + SEPARATOR + MALFORMED_GOAL_MSG + " to take on blackboard.");
        } catch (NoSolutionException e) {
            System.err.println(this.getAgentName() + SEPARATOR + NO_SOLUTION_MSG + " to take on blackboard.");
        }
    }


    //------------------------------------------
    // Agent's external action
    //------------------------------------------

    /**
     * Gets values and creates a request to put in the incoming queue.
     * @param template template to find (case of 'read' or 'take') or term to insert (case of 'write').
     * @param agent agent instance.
     * @param action action to performe ('write', 'read', 'take').
     */
    public void insertRequest(final String template, final AbstractAgent agent, final String action) {
        this.incomingRequests.add(new Request(template, agent, action));
    }


    /**
     * Defines a waiting agent.
     */
    public final class Request {
        private final String template;
        private final AbstractAgent agent;
        private final String action;

        /**
         * Constructor for the request.
         * @param template template to find (case of 'read' or 'take') or term to insert (case of 'write').
         * @param agent agent instance.
         * @param action action to performe ('write', 'read', 'take').
         */
        private Request(final String template, final AbstractAgent agent, final String action) {
            this.template = template;
            this.agent = agent;
            this.action = action;
        }

        public String getTemplate() {
            return this.template;
        }

        public AbstractAgent getAgent() {
            return this.agent;
        }

        public String getAction() {
            return this.action;
        }

    }
}
