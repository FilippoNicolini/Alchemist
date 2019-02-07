package it.unibo.alchemist.model.implementations.actions;

import alice.tuprolog.*;
import it.unibo.alchemist.model.implementations.nodes.AgentsContainerNode;
import it.unibo.alchemist.model.interfaces.Action;
import it.unibo.alchemist.model.interfaces.Context;
import it.unibo.alchemist.model.interfaces.Node;
import it.unibo.alchemist.model.interfaces.Reaction;

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

    private final String agentName; // string for the agent name
    private boolean isInitialized = false; // flag for init of the agent
    private Queue<InMessage> inbox = new LinkedList<>(); // mailbox IN queue
    private Queue<OutMessage> outbox = new LinkedList<>(); // mailbox OUT queue
    private Prolog engine = new Prolog(); // prolog engine
    private Reaction<Object> agentReaction;

    public SimpleAgentAction(final String name, final Node<Object> node) {
        super(node);
        this.agentName = name;

        // load the theory only if the agent isn't the postman
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
            final Struct self = new Struct("self", new Struct(getAgentName())); // set the name of the agent

            // initialize the agent
            // (get the theory manager and add a struct to the top)
            this.engine.getTheoryManager().assertA(self, true, null, false); // use always the last three parameters

            this.setInitialized(); // change the flag for the status of the agent

            System.out.println("Nodo: " + getNode().getId() + " || agent " + getAgentName() + " inizializzato");
            try {
                // compute the init
                final SolveInfo si = engine.solve("init.");

                if (si.isSuccess()) {
                    this.hanldeOutGoingMessages();
                }
            } catch (MalformedGoalException e) {
                System.err.println("Fail to solve 'init' for the agent " + getAgentName());
            }
        } else {
            this.handleIngoingMessages();

//            System.out.println("++++++++ Theory of agent " + getAgentName() + " +++++++++");
//            System.out.println(this.engine.getTheory());
//            System.out.println("---------------------------------------------------------");

            try {
                final SolveInfo si = engine.solve("receive.");
                long t1 = new Date().getTime();
                long threeshold = 10000;
                while (new Date().getTime() + threeshold <= t1) {
                    // do nothing..
                }

                if (si.isSuccess()) {
                    ((AgentsContainerNode) getNode()).changeNodePosition(this.agentReaction.getTau());
                    System.out.println("Agent " + getAgentName() + " has received message successfully || " + this.agentReaction.getTau().toDouble());
                    this.hanldeOutGoingMessages();
                }
            } catch (MalformedGoalException e) {
                System.err.println("Fail to solve 'receive' for the agent " + getAgentName());
            }
        }
    }

    /**
     * Prepare messages to send
     */
    public void hanldeOutGoingMessages() {
        try {
            SolveInfo si = this.engine.solve("retract(outgoing(S,R,M)).");

            while (si != null && si.isSuccess()) {
                this.addToOutbox(new OutMessage(si.getTerm("S"), si.getTerm("R"), si.getTerm("M")));
                if (si.hasOpenAlternatives()) {
                    si = this.engine.solveNext();
                } else {
                    si = null;
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
     * Insert messages received into the theory
     */
    public void handleIngoingMessages() {
        while (!this.inboxIsEmpty()) {
            final InMessage msg = this.takeFromInbox();
            final Struct ingoing = new Struct("ingoing", msg.getSender(),msg.getPayload());
            // get the theory manager and add a struct to the bottom
            this.engine.getTheoryManager().assertZ(ingoing, true, null, false); // use always the last three parameters
        }
    }

    /**
     * Take and retrieve messages to send and then clear the outbox
     * @return List<OutMessage>
     */
    public List<OutMessage> consumeOutgoingMessages() {
        final List outMessages = new ArrayList<>(this.outbox);
        this.outbox.clear();
        return outMessages;
    }

    public void receiveIncomingMessages(final List<OutMessage> ingoingMessages) {
        final List incomingMessages = new ArrayList<InMessage>();
        final List filteredMessages = ingoingMessages.stream().filter(msg -> msg.receiver.match(new Struct(getAgentName()))).collect(Collectors.toList());
        if (filteredMessages.size() > 0) {
            filteredMessages.forEach(msg -> {
                incomingMessages.add(new InMessage(((OutMessage) msg).sender, ((OutMessage) msg).payload));
            });

            this.inbox.addAll(incomingMessages);
        }
        // compact version
//        this.inbox.addAll(
//                ingoingMessages.stream()
//                .filter(msg -> msg.getReceiver().match(new Struct(getAgentName())))
//                .map(msg -> new InMessage(msg.getSender(),msg.getPayload()))
//                .collect(Collectors.toList())
//        );
    }


    //------------------------------------------
    // Class for definition of messages
    //------------------------------------------

    /**
     * Definition of an incoming message
     */
    public static class InMessage {
        private final Term sender;
        // private final Term receiver;
        private final Term payload;

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
     * Definition of an outgoing message
     */
    public static class OutMessage {
        private final Term sender;
        private final Term receiver;
        private final Term payload;

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
