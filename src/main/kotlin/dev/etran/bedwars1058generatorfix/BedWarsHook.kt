package dev.etran.bedwars1058generatorfix

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockState
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.EventExecutor
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

/**
 * BedWars1058 (GamePlayingTask, ENDER_DRAGON case) sets the 20 blocks below every team generator
 * to AIR the moment sudden death starts, which leaves a hole items fall through.
 *
 * Arena.setNextEvent fires NextEventChangeEvent (oldEvent == ENDER_DRAGON) in the same tick, just
 * BEFORE that removal loop runs. We snapshot those columns then and put them back a tick later.
 * Nothing else is touched, so blocks players break or place are never reverted.
 *
 * Reflection is used so there is no compile-time dependency on the BedWars API (its Maven repo is
 * gone) and BedWars2023 (another package root) works too.
 */
class BedWarsHook(private val plugin: GeneratorFixPlugin) {

    private val listener = object : Listener {}
    private var apiPackage: String? = null
    private var loader: ClassLoader? = null
    var pluginName: String = "?"
        private set

    fun detect(): Boolean {
        for ((name, pkg) in CANDIDATES) {
            val p = Bukkit.getPluginManager().getPlugin(name) ?: continue
            val cl = p.javaClass.classLoader
            if (runCatching { Class.forName("$pkg.arena.IArena", false, cl) }.isSuccess) {
                apiPackage = pkg
                loader = cl
                pluginName = name
                return true
            }
        }
        return false
    }

    fun register(): Boolean {
        val pkg = apiPackage ?: return false
        val cls = try {
            @Suppress("UNCHECKED_CAST")
            Class.forName("$pkg.events.gameplay.NextEventChangeEvent", false, loader) as Class<out Event>
        } catch (t: Throwable) {
            plugin.logger.log(Level.WARNING, "NextEventChangeEvent not found", t)
            return false
        }
        Bukkit.getPluginManager().registerEvent(
            cls, listener, EventPriority.MONITOR,
            EventExecutor { _, event ->
                try { onNextEvent(event) } catch (t: Throwable) {
                    plugin.logger.log(Level.WARNING, "Error handling NextEventChangeEvent", t)
                }
            },
            plugin, true,
        )
        return true
    }

    private fun onNextEvent(event: Any) {
        // getOldEvent() = the event that is happening right now. Enum compared by name.
        if (call(event, "getOldEvent")?.toString() != "ENDER_DRAGON") return
        val arena = call(event, "getArena") ?: return
        val arenaName = call(arena, "getArenaName") as? String ?: "?"

        val saved = ArrayList<BlockState>()
        for (team in list(arena, "getTeams")) {
            for (generator in list(team, "getGenerators")) {
                val loc = call(generator, "getLocation") as? Location ?: continue
                val world = loc.world ?: continue
                for (dy in 0 until plugin.depth) {
                    val block = world.getBlockAt(loc.blockX, loc.blockY - dy, loc.blockZ)
                    if (block.type != Material.AIR) saved.add(block.state)
                }
            }
        }

        if (plugin.debug) plugin.logger.info("Sudden death in '$arenaName': saved ${saved.size} generator blocks")
        if (saved.isEmpty()) return

        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            // Don't touch a world that BedWars already reset after the game ended.
            if (call(arena, "getStatus")?.toString() != "playing") return@Runnable
            var restored = 0
            for (state in saved) {
                if (state.block.type == Material.AIR) {
                    state.update(true, false) // force, no physics
                    restored++
                }
            }
            if (plugin.debug) plugin.logger.info("Restored $restored generator blocks in '$arenaName'")
        }, plugin.restoreDelayTicks)
    }

    private val cache = ConcurrentHashMap<String, Method?>()

    private fun call(target: Any?, name: String): Any? {
        if (target == null) return null
        val m = cache.getOrPut(target.javaClass.name + "#" + name) {
            target.javaClass.methods.firstOrNull { it.name == name && it.parameterCount == 0 }
                ?.also { runCatching { it.isAccessible = true } }
        } ?: return null
        return try { m.invoke(target) } catch (_: Throwable) { null }
    }

    private fun list(target: Any?, name: String): List<Any> =
        (call(target, name) as? Collection<*>)?.filterNotNull() ?: emptyList()

    private companion object {
        val CANDIDATES = listOf(
            "BedWars1058" to "com.andrei1058.bedwars.api",
            "BedWars2023" to "com.tomkeuper.bedwars.api",
        )
    }
}
