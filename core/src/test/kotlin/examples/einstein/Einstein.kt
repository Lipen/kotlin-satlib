@file:Suppress("UNUSED_VARIABLE")

package examples.einstein

import com.github.lipen.multiarray.MultiArray
import com.github.lipen.satlib.core.DomainVarArray
import com.github.lipen.satlib.core.convertDomainVarArray
import com.github.lipen.satlib.core.eq
import com.github.lipen.satlib.core.neq
import com.github.lipen.satlib.core.newDomainVarArray
import com.github.lipen.satlib.op.exactlyOne
import com.github.lipen.satlib.op.imply
import com.github.lipen.satlib.op.implyOr
import com.github.lipen.satlib.solver.Solver
import com.github.lipen.satlib.solver.addClause
import com.github.lipen.satlib.jni.solver.GlucoseSolver
import com.github.lipen.satlib.utils.useWith
import examples.utils.secondsSince
import examples.utils.timeNow

object GlobalsEinstein {
    val solverProvider: () -> Solver = {
        // MiniSatSolver()
        GlucoseSolver()
        // CryptoMiniSatSolver()
        // CadicalSolver()
    }

    init {
        solverProvider().close()
    }
}

enum class Color {
    Yellow, Blue, Red, Ivory, Green;
}

enum class Nationality {
    Norwegian, Ukrainian, Englishman, Spaniard, Japanese;
}

enum class Drink {
    Water, Tea, Milk, OrangeJuice, Coffee;
}

enum class Smoke {
    Kools, Chesterfield, OldGold, LuckyStrike, Parliament;
}

enum class Pet {
    Fox, Horse, Snails, Dog, Zebra;
}

private fun Solver.declareVariables() {
    val color = context("color") {
        newDomainVarArray(5) { Color.values().asIterable() }
    }
    val nationality = context("nationality") {
        newDomainVarArray(5) { Nationality.values().asIterable() }
    }
    val drink = context("drink") {
        newDomainVarArray(5) { Drink.values().asIterable() }
    }
    val smoke = context("smoke") {
        newDomainVarArray(5) { Smoke.values().asIterable() }
    }
    val pet = context("pet") {
        newDomainVarArray(5) { Pet.values().asIterable() }
    }
}

private fun Solver.declareConstraints() {
    println("Declaring constraints...")

    val color: DomainVarArray<Color> = context["color"]
    val nationality: DomainVarArray<Nationality> = context["nationality"]
    val drink: DomainVarArray<Drink> = context["drink"]
    val smoke: DomainVarArray<Smoke> = context["smoke"]
    val pet: DomainVarArray<Pet> = context["pet"]

    // 0. Distinctness constraints
    for (x in Color.values()) {
        exactlyOne((1..5).map { i -> color[i] eq x })
    }
    for (x in Nationality.values()) {
        exactlyOne((1..5).map { i -> nationality[i] eq x })
    }
    for (x in Drink.values()) {
        exactlyOne((1..5).map { i -> drink[i] eq x })
    }
    for (x in Smoke.values()) {
        exactlyOne((1..5).map { i -> smoke[i] eq x })
    }
    for (x in Pet.values()) {
        exactlyOne((1..5).map { i -> pet[i] eq x })
    }

    // 1. There are five houses.
    // Already encoded.

    // 2. The Englishman lives in the red house.
    // (nationality[i] = Englishman) -> (color[i] = Red)
    for (i in 1..5) {
        imply(
            nationality[i] eq Nationality.Englishman,
            color[i] eq Color.Red
        )
    }

    // 3. The Spaniard owns the dog.
    for (i in 1..5) {
        imply(
            nationality[i] eq Nationality.Spaniard,
            pet[i] eq Pet.Dog
        )
    }

    // 4. Coffee is drunk in the green house.
    for (i in 1..5) {
        imply(
            drink[i] eq Drink.Coffee,
            color[i] eq Color.Green
        )
    }

    // 5. The Ukrainian drinks tea.
    for (i in 1..5) {
        imply(
            nationality[i] eq Nationality.Ukrainian,
            drink[i] eq Drink.Tea
        )
    }

    // 6. The green house is immediately to the right of the ivory house.
    for (i in 1..4) {
        imply(
            color[i] eq Color.Ivory,
            color[i + 1] eq Color.Green
        )
    }
    addClause(color[1] neq Color.Green)

    // 7. The Old Gold smoker owns snails.
    for (i in 1..5) {
        imply(
            smoke[i] eq Smoke.OldGold,
            pet[i] eq Pet.Snails
        )
    }

    // 8. Kools are smoked in the yellow house.
    for (i in 1..5) {
        imply(
            smoke[i] eq Smoke.Kools,
            color[i] eq Color.Yellow
        )
    }

    // 9. Milk is drunk in the middle house.
    addClause(drink[3] eq Drink.Milk)

    // 10. The Norwegian lives in the first house.
    addClause(nationality[1] eq Nationality.Norwegian)

    // 11. The man who smokes Chesterfields lives in the house next to the man with the fox.
    imply(
        smoke[1] eq Smoke.Chesterfield,
        pet[2] eq Pet.Fox
    )
    imply(
        smoke[5] eq Smoke.Chesterfield,
        pet[4] eq Pet.Fox
    )
    for (i in 2..4) {
        // (smoke[i] = Chesterfields) -> (pet[i-1]=Fox OR pet[i+1]=Fox)
        implyOr(
            smoke[i] eq Smoke.Chesterfield,
            pet[i - 1] eq Pet.Fox,
            pet[i + 1] eq Pet.Fox
        )
    }

    // 12. Kools are smoked in the house next to the house where the horse is kept.
    imply(
        smoke[1] eq Smoke.Kools,
        pet[2] eq Pet.Horse
    )
    imply(
        smoke[5] eq Smoke.Kools,
        pet[4] eq Pet.Horse
    )
    for (i in 2..4) {
        implyOr(
            smoke[i] eq Smoke.Kools,
            pet[i - 1] eq Pet.Horse,
            pet[i + 1] eq Pet.Horse
        )
    }

    // 13. The Lucky Strike smoker drinks orange juice.
    for (i in 1..5) {
        imply(
            smoke[i] eq Smoke.LuckyStrike,
            drink[i] eq Drink.OrangeJuice
        )
    }

    // 14. The Japanese smokes Parliaments.
    for (i in 1..5) {
        imply(
            nationality[i] eq Nationality.Japanese,
            smoke[i] eq Smoke.Parliament
        )
    }

    // 15. The Norwegian lives next to the blue house.
    imply(
        nationality[1] eq Nationality.Norwegian,
        color[2] eq Color.Blue
    )
    imply(
        nationality[5] eq Nationality.Norwegian,
        color[4] eq Color.Blue
    )
    for (i in 2..4) {
        implyOr(
            nationality[i] eq Nationality.Norwegian,
            color[i - 1] eq Color.Blue,
            color[i + 1] eq Color.Blue
        )
    }
}

fun main() {
    // https://en.wikipedia.org/wiki/Zebra_Puzzle

    val timeStart = timeNow()

    GlobalsEinstein.solverProvider().useWith {
        declareVariables()
        declareConstraints()
        println(
            "Declared $numberOfVariables variables and $numberOfClauses clauses in %.3fs"
                .format(secondsSince(timeStart))
        )

        println("Solving...")
        if (solve()) {
            println("SAT in %.3fs".format(secondsSince(timeStart)))
            val model = getModel()

            val color: MultiArray<Color> = context.convertDomainVarArray("color", model)
            val nationality: MultiArray<Nationality> = context.convertDomainVarArray("nationality", model)
            val drink: MultiArray<Drink> = context.convertDomainVarArray("drink", model)
            val smoke: MultiArray<Smoke> = context.convertDomainVarArray("smoke", model)
            val pet: MultiArray<Pet> = context.convertDomainVarArray("pet", model)

            println("color: ${color.values}")
            println("nationality: ${nationality.values}")
            println("drink: ${drink.values}")
            println("smoke: ${smoke.values}")
            println("pet: ${pet.values}")
        } else {
            println("Unexpected UNSAT in %.3fs".format(secondsSince(timeStart)))
        }
    }

    println()
    println("All done in %.3f s!".format(secondsSince(timeStart)))
}
