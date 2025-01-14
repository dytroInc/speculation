package io.github.monun.speculation.plugin

import io.github.monun.kommand.getValue
import io.github.monun.kommand.kommand
import io.github.monun.speculation.paper.PaperGameProcess
import io.github.monun.speculation.paper.PieceColor
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class SpeculationPlugin : JavaPlugin() {
    var process: PaperGameProcess? = null
        private set

    override fun onEnable() {
        val scoreboard = Bukkit.getScoreboardManager().mainScoreboard
        for (color in PieceColor.values()) {
            val name = color.textColor.toString()
            val team = scoreboard.getTeam(name) ?: scoreboard.registerNewTeam(name)
            team.apply {
                team.color(color.textColor)
            }
            logger.info("Team ${team.name} generated")
        }


        kommand {
            register("speculation") {
                permission("speculation.commands")
                then("start") {
                    then("world" to dimension(), "players" to players(), "teamMatch" to bool()) {
                        executes {
                            val world: World by it
                            val players: Collection<Player> by it
                            val teamMatch: Boolean by it

                            kotlin.runCatching {
                                startProcess(world, players.toSet(), teamMatch)
                            }.onFailure { exception ->
                                if (exception is IllegalStateException) {
                                    feedback(Component.text(exception.message ?: "").color(NamedTextColor.RED))
                                } else {
                                    throw exception
                                }
                            }
                        }
                    }
                }
                then("stop") {
                    executes {
                        kotlin.runCatching {
                            stopProcess()
                        }.onFailure { exception ->
                            if (exception is IllegalArgumentException) {
                                feedback(Component.text(exception.message ?: "").color(NamedTextColor.RED))
                            } else {
                                throw exception
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDisable() {
        runCatching { stopProcess() }
    }

    fun startProcess(world: World, players: Set<Player>, teamMatch: Boolean): PaperGameProcess {
        check(process == null) { "process already running" }

        return PaperGameProcess(this, world).apply {
            register(players, teamMatch)
        }.also {
            process = it
        }
    }

    fun stopProcess(): PaperGameProcess {
        val process = requireNotNull(process) { "process is not running" }
        process.unregister()
        this.process = null

        return process
    }
}