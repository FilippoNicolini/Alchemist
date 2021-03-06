package it.unibo.alchemist.loader.export;

import java.util.Optional;
import java.util.Set;

import org.apache.commons.math3.stat.descriptive.UnivariateStatistic;
import org.reflections.Reflections;

/**
 * Utility to translate statistics names into a {@link UnivariateStatistic}.
 */
public final class StatUtil {

    private static final Set<Class<? extends UnivariateStatistic>> STATISTICS = new Reflections()
            .getSubTypesOf(UnivariateStatistic.class);

    private StatUtil() {
    }

    /**
     * @param name
     *            the statistic
     * @return a new instance of the corresponding {@link UnivariateStatistic}
     *         wrapped in a {@link Optional}, if one exists;
     *         {@link Optional#empty()} otherwise.
     */
    public static Optional<UnivariateStatistic> makeUnivariateStatistic(final String name) {
        return STATISTICS.stream()
            .filter(stat -> stat.getSimpleName().equalsIgnoreCase(name))
            .findAny()
            .map(clazz -> {
                try {
                    return clazz.newInstance();
                } catch (IllegalAccessException | InstantiationException e) {
                    throw new IllegalStateException("Could not initialize with empty constructor " + clazz, e);
                }
            });
    }

}
