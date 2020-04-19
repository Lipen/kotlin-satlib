@file:Suppress("FunctionName")

package examples.bf

sealed class Logic {
    abstract fun eval(values: List<Boolean>, variables: List<String>): Boolean
    abstract fun toPrettyString(): String
}

sealed class Literal : Logic() {
    object True : Literal() {
        override fun eval(values: List<Boolean>, variables: List<String>): Boolean = true
        override fun toPrettyString(): String = "⊤"
    }

    object False : Literal() {
        override fun eval(values: List<Boolean>, variables: List<String>): Boolean = false
        override fun toPrettyString(): String = "⊥"
    }
}

data class Proposition(
    val name: String
) : Logic() {
    override fun eval(values: List<Boolean>, variables: List<String>): Boolean {
        return values[variables.indexOf(name)]
    }

    override fun toPrettyString(): String {
        return name
    }
}

private fun Logic.toPrettyStringMaybeEmbraced(): String = when (this) {
    is Literal, is Proposition, is Not -> toPrettyString()
    else -> "(${toPrettyString()})"
}

sealed class UnaryOperation : Logic() {
    abstract val expr: Logic
}

data class Not(
    override val expr: Logic
) : UnaryOperation() {
    override fun eval(values: List<Boolean>, variables: List<String>): Boolean {
        return !expr.eval(values, variables)
    }

    override fun toPrettyString(): String {
        return "~${expr.toPrettyStringMaybeEmbraced()}"
    }
}

sealed class BinaryOperation : Logic() {
    abstract val lhs: Logic
    abstract val rhs: Logic
    protected abstract val prettySymbol: String

    final override fun toPrettyString(): String {
        val l = lhs.toPrettyStringMaybeEmbraced()
        val r = rhs.toPrettyStringMaybeEmbraced()
        return "$l $prettySymbol $r"
    }

    protected abstract fun eval(lhs: Boolean, rhs: Boolean): Boolean

    final override fun eval(values: List<Boolean>, variables: List<String>): Boolean {
        return eval(lhs.eval(values, variables), rhs.eval(values, variables))
    }
}

data class And(
    override val lhs: Logic,
    override val rhs: Logic
) : BinaryOperation() {
    override val prettySymbol: String = "&"
    override fun eval(lhs: Boolean, rhs: Boolean): Boolean = lhs && rhs
}

data class Or(
    override val lhs: Logic,
    override val rhs: Logic
) : BinaryOperation() {
    override val prettySymbol: String = "|"
    override fun eval(lhs: Boolean, rhs: Boolean): Boolean = lhs || rhs
}

data class Imply(
    override val lhs: Logic,
    override val rhs: Logic
) : BinaryOperation() {
    override val prettySymbol: String = "->"
    override fun eval(lhs: Boolean, rhs: Boolean): Boolean = !lhs || rhs
}

data class Iff(
    override val lhs: Logic,
    override val rhs: Logic
) : BinaryOperation() {
    override val prettySymbol: String = "<->"
    override fun eval(lhs: Boolean, rhs: Boolean): Boolean = lhs == rhs
}
