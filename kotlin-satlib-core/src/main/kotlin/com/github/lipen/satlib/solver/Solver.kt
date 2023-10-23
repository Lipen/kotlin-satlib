package com.github.lipen.satlib.solver

import com.github.lipen.satlib.core.Context
import com.github.lipen.satlib.core.Lit
import com.github.lipen.satlib.core.LitArray
import com.github.lipen.satlib.core.Model
import com.github.lipen.satlib.core.SequenceScopeLit
import com.github.lipen.satlib.utils.toList_
import java.io.File

/**
 * Generic SAT solver.
 *
 * - Use [newLiteral] to declare new variables.
 * - Use [addClause] to declare new clauses.
 * - Use [context] to store any additional data.
 * - Use [assumptions] to add assumptions for the next call to [solve].
 * - Use [solve] to solve the SAT problem.
 * - Use [getValue] to query the value of literals (though, it is advisable to use [Model] for this).
 * - Use [getModel] to query the satisfying assignment (model).
 */
interface Solver : AutoCloseable {
    /**
     * The context.
     */
    var context: Context

    /**
     * Number of variables added (via [newLiteral]) to the SAT solver.
     */
    val numberOfVariables: Int

    /**
     * Number of clauses added (via [addClause]) to the SAT solver.
     */
    val numberOfClauses: Int

    // TODO: doc
    val assumptions: MutableList<Lit>

    /**
     * Reset the solver.
     *
     * The implementation of [Solver] should ensure that after [reset] the [context] is cleared,
     * [numberOfVariables] and [numberOfClauses] are zeroed, and the solver is ready for new problems.
     */
    fun reset()

    /**
     * "Close" the solver.
     *
     * This effectively closes all "connections", "streams" and "buffers",
     * releases the acquired resources and performs the cleanup.
     * In general, the solver becomes unusable after the call to [close],
     * and can be restored to the working state via [reset].
     */
    override fun close()

    /**
     * Interrupt the SAT solver.
     *
     * In general, after the SAT solver was interrupted, the call to [solve] returns `false`.
     * Note that the solving process may not stop immediately, since this operation is asynchronous.
     * Due to this, the call to [solve] may have time to return `true` if you happen
     * to call [interrupt] when the solver is about to actually solve the SAT problem.
     */
    fun interrupt()

    /**
     * Dump the constructed CNF in DIMACS format to the file.
     */
    fun dumpDimacs(file: File)

    /**
     * Add a comment.
     *
     * DIMACS format supports comments as lines starting with "c", e.g. "c this is a comment".
     */
    fun comment(comment: String)

    /**
     * Create (allocate) a new SAT variable.
     *
     * The returned value is always a positive literal.
     *
     * Some solvers require an explicit call to allocate a new variable,
     * while others do this allocation implicitly when adding new clauses to the solver.
     * However, it is advisable to always manually call [newLiteral] beforehand,
     * instead of using arbitrary [Lit]s.
     */
    fun newLiteral(): Lit

    // TODO: doc
    fun addClause(literals: List<Lit>)

    // TODO: doc
    fun solve(): Boolean

    /**
     * Query the Boolean value of a literal.
     *
     * **Note:** the solver should be in the SAT state.
     * The result of [getValue] when the solver is not in the SAT state
     * depends on the backend implementation.
     */
    fun getValue(lit: Lit): Boolean

    /**
     * Query the satisfying assignment (model) for the SAT problem.
     *
     * In general, the Solver implementations construct the [Model] on each call to [getModel].
     * The model could have the large size, so make sure to call this method only once.
     *
     * **Note:** the solver should be in the SAT state.
     * Some Solver implementations return the latest model (cached)
     * even when the solver is already not in the SAT state (due to possibly new added clauses),
     * but it is advisable to query the model right after the call to [solve] which returned `true`.
     */
    fun getModel(): Model
}

inline fun Solver.switchContext(newContext: Context, block: () -> Unit) {
    val oldContext = this.context
    this.context = newContext
    block()
    this.context = oldContext
}

//region [addClause]

fun Solver.addClause(literals: Iterable<Lit>) {
    addClause(literals.toList_())
}

fun Solver.addClause(literals: LitArray) {
    addClause(literals.asList())
}

@JvmName("addClauseVararg")
fun Solver.addClause(vararg literals: Lit) {
    addClause(literals)
}

fun Solver.addClause(literals: Sequence<Lit>) {
    addClause(literals.asIterable())
}

fun Solver.addClause(block: SequenceScopeLit) {
    addClause(sequence(block).constrainOnce())
}

//endregion

//region [assume]

fun Solver.assume(literals: Iterable<Lit>) {
    assumptions.addAll(literals)
}

fun Solver.assume(literals: Sequence<Lit>) {
    assume(literals.asIterable())
}

fun Solver.assume(literals: LitArray) {
    assume(literals.asList())
}

@JvmName("assumeVararg")
fun Solver.assume(vararg literals: Lit) {
    assume(literals)
}

//endregion

//region [solve]

fun Solver.solve(assumptions: Iterable<Lit>): Boolean {
    assume(assumptions)
    return solve()
}

fun Solver.solve(assumptions: Sequence<Lit>): Boolean {
    return solve(assumptions.asIterable())
}

fun Solver.solve(assumptions: LitArray): Boolean {
    return solve(assumptions.asList())
}

@JvmName("solveVararg")
fun Solver.solve(vararg assumptions: Lit): Boolean {
    return solve(assumptions)
}

//endregion
