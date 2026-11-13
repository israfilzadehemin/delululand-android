package com.delulutopia.feature.start

import com.delulutopia.feature.start.PinViewModel.Problem

/**
 * C4 and J2: "Create your PIN" then "Confirm your PIN". The sixth confirm digit submits; a refusal
 * starts the PIN over with its text. Pure state, owned by the wizard's ViewModel.
 */
data class WizardPin(
    val confirming: Boolean = false,
    val first: String = "",
    val digits: String = "",
    val problem: Problem? = null,
    val busy: Boolean = false,
) {
    fun type(d: String) = copy(digits = d, problem = if (d.isNotEmpty()) null else problem)

    /** The PIN to submit once both entries match, else null with the next state in [next]. */
    fun complete(pin: String): Pair<WizardPin, String?> = when {
        !confirming -> copy(confirming = true, first = pin, digits = "", problem = null) to null
        pin != first -> restart(Problem.Differ) to null
        else -> copy(busy = true, problem = null) to pin
    }

    fun restart(problem: Problem?) = WizardPin(problem = problem)
}
