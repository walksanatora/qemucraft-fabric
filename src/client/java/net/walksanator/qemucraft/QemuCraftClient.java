package net.walksanator.qemucraft;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.walksanator.qemucraft.blocks.TerminalEntity;
import net.walksanator.qemucraft.gui.TerminalScreen;
import net.walksanator.qemucraft.init.Packets;

public class QemuCraftClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.

		Shaders.INSTANCE.init();

		ClientPlayNetworking.registerGlobalReceiver(Packets.Client.INSTANCE.getOPEN_TERMINAL(), (client, handler, buf, responseSender) -> {
			if (client.world.getBlockEntity(buf.readBlockPos()) instanceof TerminalEntity te) {
				client.execute(() -> {
					client.setScreen(
							new TerminalScreen(te)
					);
				});
			}

		});
	}
}