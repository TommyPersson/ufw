package io.tpersson.ufw.durablejobs.periodic.internal

public interface PeriodicJobSpecsProvider {
    public val periodicJobSpecs: List<PeriodicJobSpec<*>>
}