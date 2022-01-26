package nexus.aig

import mu.KotlinLogging

private val log = KotlinLogging.logger {}

fun toposort(deps: Map<Int, Collection<Int>>): Sequence<List<Int>> = sequence {
    log.debug { "Performing a topological sort" }

    var data = deps

    while (true) {
        val layer = data.mapNotNull { (id, dep) -> if (dep.isEmpty()) id else null }
        if (layer.isEmpty()) break

        yield(layer)

        data = data.mapNotNull { (id, dep) ->
            if (id in layer) {
                null
            } else {
                id to (dep - layer)
            }
        }.associateTo(mutableMapOf()) { it }
    }

    check(data.isEmpty()) { "Circular dependency detected: $data" }
}
