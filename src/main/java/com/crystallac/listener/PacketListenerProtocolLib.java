package com.crystallac.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.crystallac.CrystallAC;
import com.crystallac.check.CheckManager;
import com.crystallac.data.PlayerData;
import com.crystallac.data.PlayerDataManager;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Intercepts low-level incoming and outgoing network packets via ProtocolLib
 * to ensure microsecond-level timing and rotation precision across all supported versions.
 */
public class PacketListenerProtocolLib {

    private final CrystallAC plugin;
    private final PlayerDataManager dataManager;
    private final CheckManager checkManager;

    public PacketListenerProtocolLib(CrystallAC plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.checkManager = plugin.getCheckManager();
    }

    public void register() {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

        List<PacketType> clientFlyingTypes = new ArrayList<>();
        if (PacketType.Play.Client.POSITION.isSupported()) clientFlyingTypes.add(PacketType.Play.Client.POSITION);
        if (PacketType.Play.Client.POSITION_LOOK.isSupported()) clientFlyingTypes.add(PacketType.Play.Client.POSITION_LOOK);
        if (PacketType.Play.Client.LOOK.isSupported()) clientFlyingTypes.add(PacketType.Play.Client.LOOK);
        if (PacketType.Play.Client.GROUND.isSupported()) clientFlyingTypes.add(PacketType.Play.Client.GROUND);

        if (clientFlyingTypes.isEmpty()) {
            return;
        }

        protocolManager.addPacketListener(new PacketAdapter(
                plugin,
                ListenerPriority.MONITOR,
                clientFlyingTypes
        ) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                if (player == null) return;

                PlayerData data = dataManager.getPlayerData(player);
                if (data != null) {
                    checkManager.dispatchFlyingPacket(data);
                }
            }
        });
    }
}
