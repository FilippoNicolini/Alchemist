package it.unibo.alchemist.model.implementations.actions;

import alice.tuprolog.*;
import it.unibo.alchemist.model.implementations.nodes.AgentsContainerNode;
import it.unibo.alchemist.model.interfaces.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

public class SimpleAgentAction extends AbstractAction<Object> {

    private final String agentName; // String for the agent name
    private boolean isInitialized = false; // Flag for init of the agent
    private Queue<InMessage> inbox = new LinkedList<>(); // Mailbox IN queue
    private Queue<OutMessage> outbox = new LinkedList<>(); // Mailbox OUT queue
    private Prolog engine = new Prolog(); // tuProlog engine
    private Reaction<Object> agentReaction; // Reference to reaction

    public SimpleAgentAction(final String name, final Node<Object> node) {
        super(node);
        this.agentName = name;

        // Loads the theory only if the agent isn't the postman
        if (!"postman".equals(this.agentName)) {
            try {
                this.engine.setTheory(new Theory(new FileInputStream(new File("alchemist-incarnation-agent/src/main/resources/" + getAgentName() + ".pl"))));
            } catch (IOException e) {
                System.err.println("Fail reading file of agent " + getAgentName());
            } catch (InvalidTheoryException e) {
                System.err.println("The agent theory " + getAgentName() + " is not valid.");
            }
        }
    }

    @Override
    public Action<Object> cloneAction(final Node<Object> n, final Reaction<Object> r) {
        return null; // TODO come implementare?
    }

    @Override
    public Context getContext() {
        return Context.NEIGHBORHOOD; // TODO va bene come profondità?
    }

    private void setInitialized() {
        this.isInitialized = true;
    }

    private boolean isInitialized() {
        return isInitialized;
    }

    public String getAgentName() {
        return agentName;
    }

    public void addToOutbox(final OutMessage m) {
        this.outbox.add(m);
    }

    public boolean inboxIsEmpty() {
        return this.inbox.size() == 0;
    }

    public InMessage takeFromInbox() {
        return this.inbox.poll();
    }

    public void setAgentReaction(final Reaction reaction) {
        this.agentReaction = reaction;
    }

    @Override
    public void execute() {
        if (!this.isInitialized()) {
            // Set the name of the agent
            final Struct self = new Struct("self", new Struct(getAgentName()));

            // Initialize the agent
            // (get the theory manager and add a struct to the top)
            this.engine.getTheoryManager().assertA(self, true, null, false); // use always these last three parameters

            // Updates the initialization flag
            this.setInitialized();

            System.out.println("Nodo: " + getNode().getId() + " || agent " + getAgentName() + " inizializzato");
            try {
                // Computes the init (e.g. In ping/pong problem send the first message).
                final SolveInfo init = this.engine.solve("init.");

                if (init.isSuccess()) {
                    this.hanldeOutGoingMessages();
                }
            } catch (MalformedGoalException e) {
                System.err.println("Fail to solve 'init' for the agent " + getAgentName());
            }
        } else {
            //Agent's reasoning cycle

            // (1) Handles incoming messages
            this.handleIncomingMessages();

            //this.timeLossSimulation(0);

            // Moves the node
            ((AgentsContainerNode) getNode()).changeNodePosition(this.agentReaction.getTau());

            // (2) Checks plan in the theory
            try {
                // (2.1) Receiving messages
                final SolveInfo receive = this.engine.solve("receive.");
                if (receive.isSuccess()) {
                    System.out.println("Agent " + getAgentName() + " has received message successfully || " + this.agentReaction.getTau().toDouble());
                }
            } catch (MalformedGoalException e) {
                System.err.println("Fail to solve 'receive' in reasoning cycle for the agent " + getAgentName());
            }

            try {
                // (2.2) Perceives the environment
                // Verifies node position and eventually change direction
                final Position currPos = ((AgentsContainerNode)getNode()).getNodePosition();
                final SolveInfo checkPos = this.engine.solve("checkPosition(" + currPos.getCoordinate(0) + "," + currPos.getCoordinate(1) + ").");
                if (checkPos.isSuccess()) {
                    try {
                        // Checks if limits have been reached
                        SolveInfo limits = this.engine.solve("retract(reachedLimit(X)).");
                        while (limits != null && limits.isSuccess()) {
                            /* Possible values of X:
                             * - T: reached upper limit
                             * - R: reached right limit
                             * - B: reached bottom limit
                             * - L: reached left limit
                             */
                            System.out.println(this.getAgentName() + " reached limit: " + limits.getTerm("X") + "");
                            ((AgentsContainerNode) getNode()).changeDirectionAngle((int)(Math.random()*360), true, this.agentReaction.getTau()); // TODO modificare la direzione in modo sensato
                            if (limits.hasOpenAlternatives()) {
                                limits = this.engine.solveNext();
                            } else {
                                limits = null;
                            }
                        }
                    } catch (MalformedGoalException e) {
                        System.err.println("Fail to solve 'reachedLimit' in reasoning cycle for the agent " + getAgentName());
                    } catch (NoSolutionException e) {
                        System.err.println("Impossible get the solution of 'reachedLimit' for the agent " + getAgentName());
                    } catch (UnknownVarException e) {
                        System.err.println("Impossible get the term in the solution of 'reachedLimit' for the agent " + getAgentName());
                    } catch (NoMoreSolutionException e) {
                        System.err.println("Impossible get the next solution of 'reachedLimit' for the agent " + getAgentName());
                    }
                }
            } catch (MalformedGoalException e) {
                System.err.println("Fail to solve 'checkPosition' in reasoning cycle for the agent " + getAgentName());
            }

            // (3) Handles outgoing messages
            this.hanldeOutGoingMessages();
        }
    }

    /**
     * Prepares messages to send
     */
    public void hanldeOutGoingMessages() {
        try {
            SolveInfo outgoing = this.engine.solve("retract(outgoing(S,R,M)).");
            while (outgoing != null && outgoing.isSuccess()) {
                this.addToOutbox(new OutMessage(outgoing.getTerm("S"), outgoing.getTerm("R"), outgoing.getTerm("M")));
                if (outgoing.hasOpenAlternatives()) {
                    outgoing = this.engine.solveNext();
                } else {
                    outgoing = null;
                }
            }
        } catch (MalformedGoalException e) {
            System.err.println("Fail to solve 'outgoing' for the agent " + getAgentName());
        } catch (NoSolutionException e) {
            System.err.println("Impossible get the solution of 'outgoing' for the agent " + getAgentName());
        } catch (UnknownVarException e) {
            System.err.println("Impossible get the term in the solution of 'outgoing' for the agent " + getAgentName());
        } catch (NoMoreSolutionException e) {
            System.err.println("Impossible get the next solution of 'outgoing' for the agent " + getAgentName());
        }
    }

    /**
     * Inserts messages received into the theory
     */
    public void handleIncomingMessages() {
        while (!this.inboxIsEmpty()) {
            final InMessage msg = this.takeFromInbox();
            final Struct incoming = new Struct("ingoing", msg.getSender(),msg.getPayload());
            // (get the theory manager and add a struct to the bottom)
            this.engine.getTheoryManager().assertZ(incoming, true, null, false); // use always the last three parameters
        }
    }

    /**
     * Takes outgoing messages to send, clears the outbox and then returns messages
     * @return List<OutMessage>
     */
    public List<OutMessage> consumeOutgoingMessages() {
        final List outMessages = new ArrayList<>(this.outbox);
        this.outbox.clear();
        return outMessages;
    }

    /**
     * Takes the messages addressed to this agent and puts them in the incoming queue
     * @param incomingMessages
     */
    public void receiveIncomingMessages(final List<OutMessage> incomingMessages) {
        final List tmpIncoming = new ArrayList<InMessage>();
        final List filteredMessages = incomingMessages.stream().filter(msg -> msg.receiver.match(new Struct(getAgentName()))).collect(Collectors.toList());
        if (filteredMessages.size() > 0) {
            filteredMessages.forEach(msg -> {
                tmpIncoming.add(new InMessage(((OutMessage) msg).sender, ((OutMessage) msg).payload));
            });
            this.inbox.addAll(tmpIncoming);
        }
        // compact version
//        this.inbox.addAll(
//                incomingMessages.stream()
//                .filter(msg -> msg.getReceiver().match(new Struct(getAgentName())))
//                .map(msg -> new InMessage(msg.getSender(),msg.getPayload()))
//                .collect(Collectors.toList())
//        );
    }

    // TODO only for testing
    private void timeLossSimulation(final long period) {
        long t1 = new Date().getTime();
        while (new Date().getTime() + period <= t1) {
            // do nothing..
        }
    }


    //------------------------------------------
    // Class for definition of messages
    //------------------------------------------

    /**
     * Defines an incoming message
     */
    public static class InMessage {
        private final Term sender;
        // private final Term receiver;
        private final Term payload;

        /**
         * Constructor for incoming messages
         * @param sender
         * @param payload
         */
        public InMessage(final Term sender, final Term payload) {
            this.sender = sender;
            this.payload = payload;
        }

        public Term getSender() {
            return sender;
        }

        public Term getPayload() {
            return payload;
        }
    }

    /**
     * Defines an outgoing message
     */
    public static class OutMessage {
        private final Term sender;
        private final Term receiver;
        private final Term payload;

        /**
         * Constructor for outgoing messages
         * @param sender
         * @param receiver
         * @param payload
         */
        public OutMessage(final Term sender, final Term receiver, final Term payload) {
            this.sender = sender;
            this.receiver = receiver;
            this.payload = payload;
        }

        public Term getSender() {
            return sender;
        }

        public Term getReceiver() {
            return receiver;
        }

        public Term getPayload() {
            return payload;
        }
    }
}
