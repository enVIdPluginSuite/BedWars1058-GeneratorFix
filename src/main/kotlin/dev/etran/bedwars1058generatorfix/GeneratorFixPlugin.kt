package dev.etran.bedwars1058generatorfix

import org.bukkit.plugin.java.JavaPlugin

class GeneratorFixPlugin : JavaPlugin() {

    var depth: Int = 20
        private set
    var restoreDelayTicks: Long = 1L
        private set
    var debug: Boolean = false
        private set

    override fun onEnable() {
        saveDefaultConfig()
        depth = config.getInt("depth", 20).coerceIn(1, 256)
        restoreDelayTicks = config.getLong("restore-delay-ticks", 1L).coerceAtLeast(1L)
        debug = config.getBoolean("debug", false)

        val hook = BedWarsHook(this)
        if (!hook.detect()) {
            logger.severe("BedWars1058 (or BedWars2023) was not found. Disabling.")
            server.pluginManager.disablePlugin(this)
            return
        }
        if (hook.register()) {
            logger.info("Hooked into ${hook.pluginName}: generator bases will be kept intact at sudden death.")
        } else {
            logger.severe("Could not hook the BedWars sudden-death event. Disabling.")
            server.pluginManager.disablePlugin(this)
        }
    }
}
