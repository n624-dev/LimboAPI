/* SPDX-License-Identifier: AGPL-3.0-or-later */
package jp.n624.limbo.validation;

import java.net.InetSocketAddress;
import io.netty.buffer.Unpooled;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.PacketErrorEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.factory.ClientNetworkSessionFactory;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.MinecraftConstants;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftTypes;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundLevelChunkWithLightPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundPlayerLoadedPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;

/** Offline protocol probe using the exact codec bundled with the tested Geyser JAR. */
public final class ProtocolProbe {
  public static void main(String[] args) throws Exception {
    String name = args.length > 0 ? args[0] : "Smoke263";
    int seconds = args.length > 1 ? Integer.parseInt(args[1]) : 25;
    MinecraftProtocol protocol = new MinecraftProtocol(name);
    if (protocol.getCodec().getProtocolVersion() != 777) {
      throw new IllegalStateException("Expected protocol 777, got " + protocol.getCodec().getProtocolVersion());
    }
    var session = ClientNetworkSessionFactory.factory()
        .setRemoteSocketAddress(new InetSocketAddress("127.0.0.1", 25563))
        .setProtocol(protocol).create();
    session.setFlag(MinecraftConstants.SEND_BLANK_KNOWN_PACKS_RESPONSE, true);
    AtomicInteger logins = new AtomicInteger();
    AtomicInteger chunks = new AtomicInteger();
    AtomicInteger teleports = new AtomicInteger();
    AtomicInteger errors = new AtomicInteger();
    AtomicBoolean highState = new AtomicBoolean();
    AtomicBoolean finished = new AtomicBoolean();
    AtomicBoolean earlyDisconnect = new AtomicBoolean();
    session.addListener(new SessionAdapter() {
      @Override public void packetReceived(Session current, Packet packet) {
        if (packet instanceof ClientboundLoginPacket) {
          System.out.println("PROBE_LOGIN " + logins.incrementAndGet());
        } else if (packet instanceof ClientboundLevelChunkWithLightPacket chunk) {
          chunks.incrementAndGet();
          if (chunk.getX() == 0 && chunk.getZ() == 0) {
            var buffer = Unpooled.wrappedBuffer(chunk.getChunkData());
            try {
              int sections = 0;
              while (buffer.isReadable()) {
                if (++sections > 64) {
                  throw new IllegalStateException("Unexpected chunk section count");
                }
                var section = MinecraftTypes.readChunkSection(buffer, 16, 7);
                if (section.getBlock(0, 15, 0) == 35722) {
                  highState.set(true);
                }
              }
            } finally {
              buffer.release();
            }
          }
        } else if (packet instanceof ClientboundPlayerPositionPacket position) {
          var coordinates = position.getPosition();
          current.send(new ServerboundAcceptTeleportationPacket(position.getId(), coordinates.getX(),
              coordinates.getY(), coordinates.getZ(), position.getYRot(), position.getXRot()));
          current.send(ServerboundPlayerLoadedPacket.INSTANCE);
          System.out.println("PROBE_TELEPORT " + teleports.incrementAndGet());
        }
      }

      @Override public void packetError(PacketErrorEvent event) {
        errors.incrementAndGet();
        event.getCause().printStackTrace();
      }

      @Override public void disconnected(DisconnectedEvent event) {
        System.out.println("PROBE_DISCONNECTED " + event.getReason());
        if (event.getCause() != null) {
          event.getCause().printStackTrace();
        }
        if (!finished.get()) {
          earlyDisconnect.set(true);
        }
      }
    });
    session.connect(true);
    Thread.sleep(seconds * 1000L);
    boolean alive = session.isConnected();
    finished.set(true);
    session.disconnect("Protocol validation complete");
    System.out.printf("PROBE_SUMMARY protocol=777 logins=%d chunks=%d teleports=%d errors=%d alive=%s highState=%s%n",
        logins.get(), chunks.get(), teleports.get(), errors.get(), alive, highState.get());
    boolean success = alive && !earlyDisconnect.get() && errors.get() == 0
        && logins.get() >= 1 && chunks.get() > 0 && teleports.get() >= 1 && highState.get();
    System.exit(success ? 0 : 1);
  }
}
