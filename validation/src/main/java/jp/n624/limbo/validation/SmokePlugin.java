/* SPDX-License-Identifier: AGPL-3.0-or-later */
package jp.n624.limbo.validation;

import com.google.inject.Inject;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.LimboSessionHandler;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.event.LoginLimboRegisterEvent;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

/** Isolated validation only: do not install this automatic-transfer plugin in production. */
@Plugin(id = "limboapi-validation", name = "LimboAPI Validation", version = "1.0",
    dependencies = {@Dependency(id = "limboapi")})
public final class SmokePlugin {
  private final ProxyServer proxy;
  private final Logger logger;
  private final Map<UUID, Integer> connections = new ConcurrentHashMap<>();
  private Limbo limbo;

  @Inject public SmokePlugin(ProxyServer proxy, Logger logger) {
    this.proxy = proxy;
    this.logger = logger;
  }

  @Subscribe public void initialize(ProxyInitializeEvent event) {
    LimboFactory factory = (LimboFactory) proxy.getPluginManager().getPlugin("limboapi")
        .orElseThrow().getInstance().orElseThrow();
    var world = factory.createVirtualWorld(Dimension.OVERWORLD, 0.5, 81, 0.5, 0, 0);
    for (int x = -2; x <= 2; ++x) {
      for (int z = -2; z <= 2; ++z) {
        world.setBlock(x, 79, z, factory.createSimpleBlock("minecraft:stone"));
      }
    }
    world.setBlock(0, 79, 0, factory.createSimpleBlock((short) 35722, true));
    world.fillSkyLight(15);
    limbo = factory.createLimbo(world).setName("26.3 validation").setReadTimeout(60000);
    proxy.getCommandManager().register("limbo-test", (SimpleCommand) invocation -> {
      if (invocation.source() instanceof Player player) {
        spawn(player);
      }
    });
    logger.info("VALIDATION_READY high_state_id=35722");
  }

  @Subscribe public void login(LoginLimboRegisterEvent event) {
    event.addOnJoinCallback(() -> spawn(event.getPlayer()));
  }

  private void spawn(Player player) {
    limbo.spawnPlayer(player, new LimboSessionHandler() {
      @Override public void onSpawn(Limbo server, LimboPlayer limboPlayer) {
        limboPlayer.disableFalling();
        player.sendMessage(Component.text("Limbo 26.3 validation: waiting 5 seconds before Paper transfer"));
        logger.info("VALIDATION_SPAWN player={} protocol={}", player.getUsername(), player.getProtocolVersion());
        proxy.getScheduler().buildTask(SmokePlugin.this, () -> {
          if (player.isActive()) {
            logger.info("VALIDATION_TRANSFER player={}", player.getUsername());
            limboPlayer.disconnect(proxy.getServer("backend").orElseThrow());
          }
        }).delay(Duration.ofSeconds(5)).schedule();
      }

      @Override public void onTeleport(int id) {
        logger.info("VALIDATION_TELEPORT player={} id={}", player.getUsername(), id);
      }
    });
  }

  @Subscribe public void connected(ServerPostConnectEvent event) {
    Player player = event.getPlayer();
    int count = connections.merge(player.getUniqueId(), 1, Integer::sum);
    logger.info("VALIDATION_BACKEND player={} count={}", player.getUsername(), count);
    if (player.getUsername().startsWith("Smoke") && count == 1) {
      proxy.getScheduler().buildTask(this, () -> {
        if (player.isActive()) {
          spawn(player);
        }
      }).delay(Duration.ofSeconds(3)).schedule();
    }
  }

  @Subscribe public void disconnected(DisconnectEvent event) {
    connections.remove(event.getPlayer().getUniqueId());
  }
}
