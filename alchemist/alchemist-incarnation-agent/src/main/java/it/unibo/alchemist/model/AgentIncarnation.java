package it.unibo.alchemist.model;

import com.google.common.collect.Lists;
import it.unibo.alchemist.model.implementations.actions.AbstractAgent;
import it.unibo.alchemist.model.implementations.actions.Blackboard;
import it.unibo.alchemist.model.implementations.actions.Goldmine;
import it.unibo.alchemist.model.implementations.actions.MovementAgent;
import it.unibo.alchemist.model.implementations.actions.PostmanAgent;
import it.unibo.alchemist.model.implementations.actions.SimpleAgent;
import it.unibo.alchemist.model.implementations.conditions.AbstractCondition;
import it.unibo.alchemist.model.implementations.environments.Continuous2DEnvironment;
import it.unibo.alchemist.model.implementations.molecules.SimpleMolecule;
import it.unibo.alchemist.model.implementations.nodes.AgentsContainerNode;
import it.unibo.alchemist.model.implementations.reactions.AgentReaction;
import it.unibo.alchemist.model.implementations.timedistributions.DiracComb;
import it.unibo.alchemist.model.interfaces.Action;
import it.unibo.alchemist.model.interfaces.Condition;
import it.unibo.alchemist.model.interfaces.Context;
import it.unibo.alchemist.model.interfaces.Environment;
import it.unibo.alchemist.model.interfaces.Incarnation;
import it.unibo.alchemist.model.interfaces.Molecule;
import it.unibo.alchemist.model.interfaces.Node;
import it.unibo.alchemist.model.interfaces.Position;
import it.unibo.alchemist.model.interfaces.Reaction;

import it.unibo.alchemist.model.interfaces.TimeDistribution;


import org.apache.commons.math3.random.RandomGenerator;

/**
 * Class that implement agent incarnation.
 * @param <P> position
 */
public class AgentIncarnation<P extends Position<? extends P>> implements Incarnation<Object, P> {

    /**
     * Variable to indicate postman agent.
     */
    public static final String POSTMAN_AGENT_NAME = "postman";
    /**
     * Variable to indicate blackboard agent.
     */
    public static final String BLACKBOARD_AGENT_NAME = "blackboard";
    /**
     * Variable to indicate goldmine agent.
     */
    public static final String GOLDMINE_AGENT_NAME = "goldmine";

    /**
     * Get the concentration of a molecule.
     * @param node
     *            the node
     * @param mol
     *            the molecule to analyze
     * @param prop
     *            the property to extract
     * @return double of concentration.
     */
    @Override
    public double getProperty(final Node<Object> node, final Molecule mol, final String prop) {
        return (double) node.getConcentration(mol);
    }

    /**
     * Create a SimpleMolecule.
     * @param s
     *            the {@link String} to parse
     * @return the molecule created.
     */
    @Override
    public Molecule createMolecule(final String s) {
        return new SimpleMolecule(s);
    }

    /**
     * Create the concentration.
     * @param s the {@link String} to parse
     * @return the concentration created.
     */
    @Override
    public Object createConcentration(final String s) {
        return s;
    }

    /**
     * Create the node for the agent.
     * @param rand
     *            the random engine
     * @param env
     *            the environment that will host this object
     * @param param
     *            a {@link String} describing the object
     * @return the node created with the movement reaction
     */
    @Override
    public Node<Object> createNode(final RandomGenerator rand, final Environment<Object, P> env, final String param) {
        // Create the node
        final Node<Object> node = new AgentsContainerNode(param, (Environment<Object, Position<? extends Continuous2DEnvironment>>) env, rand);

        // Create the time distribution for the reaction
        final TimeDistribution<Object> td = this.createTimeDistribution(rand, env, node, "1");

        // Create the default reaction for the node movement and assign it the condition and the action
        final Reaction<Object> reaction = new AgentReaction("movementReact", node, td);
        final Condition<Object> condition = this.createCondition(rand, env, node, td, reaction, "");
        final Action<Object> action = new MovementAgent("movement", node, rand, reaction);
        reaction.setConditions(Lists.newArrayList(condition));
        reaction.setActions(Lists.newArrayList(action));
        // Add the reaction to the node
        node.addReaction(reaction);
        return node;
    }

    /**
     * Create time distribution for the reaction.
     * @param rand
     *            the random engine
     * @param env
     *            the environment that will host this object
     * @param node
     *            the node that will host this object
     * @param param
     *            a {@link String} describing the object
     * @return a DiraComb created with the retrieved param
     */
    @Override
    public TimeDistribution<Object> createTimeDistribution(final RandomGenerator rand, final Environment<Object, P> env, final Node<Object> node, final String param) {
        System.out.println("Nodo: " + node.getId() + " || createTimeDistribution || param: " + param + "\n");
        return new DiracComb<>(Double.parseDouble(param)); // Generates a dirac comb with a value (random or taken from config)
    }

    /**
     * Create the reaction.
     * @param rand
     *            the random engine
     * @param env
     *            the environment that will host this object
     * @param node
     *            the node that will host this object
     * @param time
     *            the time distribution of the reaction
     * @param param
     *            a {@link String} describing the object
     * @return an AgentReaction with a simple condition and the agent action.
     */
    @Override
    public Reaction<Object> createReaction(final RandomGenerator rand, final Environment<Object, P> env, final Node<Object> node, final TimeDistribution<Object> time, final String param) {
        System.out.println("Nodo: " + node.getId() + " || createReaction || param: " + param + "\n");

        final Reaction<Object> reaction = new AgentReaction(param + "React", node, time);
        final Condition<Object> condition = createCondition(rand, env, node, time, reaction, param); // Create condition
        final Action<Object> action = createAction(rand, env, node, time, reaction, param); // Create action

        ((AgentsContainerNode) node).addAgent((AbstractAgent) action); // Add the agent to the map of the node

        reaction.setConditions(Lists.newArrayList(condition));
        reaction.setActions(Lists.newArrayList(action));

        return reaction;
    }

    /**
     * Create a condition for the reaction.
     * @param rand
     *            the random engine
     * @param env
     *            the environment that will host this object
     * @param node
     *            the node that will host this object
     * @param time
     *            the time distribution of the reaction
     * @param reaction
     *            the reaction hosting this object
     * @param param
     *            a {@link String} describing the object
     * @return a simple implementation of AbstractCondition
     */
    @Override
    public Condition<Object> createCondition(final RandomGenerator rand, final Environment<Object, P> env, final Node<Object> node, final TimeDistribution<Object> time, final Reaction<Object> reaction, final String param) {
        System.out.println("Nodo: " + node.getId() + " || createCondition || param: " + param + "\n");
        return new AbstractCondition<Object>(node) {
            @Override
            public Context getContext() {
                // Define the depth of an action and it affects the performances
                return Context.LOCAL; // TODO va bene come profondità?
            }

            @Override
            public double getPropensityContribution() {
                return 1;
            }

            @Override
            public boolean isValid() {
                return true; // TODO per adesso va bene così, deve sempre avvenire
            }
        };
    }

    /**
     * Create the action for the agent.
     * @param rand
     *            the random engine
     * @param env
     *            the environment that will host this object
     * @param node
     *            the node that will host this object
     * @param time
     *            the time distribution of the reaction
     * @param reaction
     *            the reaction hosting this object
     * @param param
     *            a {@link String} describing the object
     * @return the instance of the class specified in the param.
     */
    @Override
    public Action<Object> createAction(final RandomGenerator rand, final Environment<Object, P> env, final Node<Object> node, final TimeDistribution<Object> time, final Reaction<Object> reaction, final String param) {
        System.out.println("Nodo: " + node.getId() + " || createAction || param: " + param + "\n");

        Action<Object> action;
        if (param.contains(POSTMAN_AGENT_NAME)) {
            action = new PostmanAgent(param, node, rand);
        } else if (param.contains(BLACKBOARD_AGENT_NAME)) {
            action = new Blackboard(param, node, rand);
        } else if (param.contains(GOLDMINE_AGENT_NAME)) {
            action = new Goldmine(param, node, rand);
        } else {
            action = new SimpleAgent(param, node, rand, reaction);
        }
        return action;
    }
}
