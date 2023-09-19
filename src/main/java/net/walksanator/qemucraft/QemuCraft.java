package net.walksanator.qemucraft;

import net.fabricmc.api.ModInitializer;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.walksanator.qemucraft.blocks.TerminalEntity;
import net.walksanator.qemucraft.init.*;
import net.walksanator.qemucraft.peripherals.CorePeriph;
import net.walksanator.qemucraft.peripherals.InternetPeriph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.function.BiFunction;

public class QemuCraft implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final String MOD_ID = "qemucraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static Resources resources;

	public static Blocks blocks = new Blocks();
	public static BlockEntityTypes blockEntityTypes = new BlockEntityTypes(blocks);
	public static Items items = new Items(blocks);

	public static HashMap<
			Identifier,
			BiFunction<TerminalEntity, NbtCompound, Peripheral>
			> peripherals = new HashMap<>();

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		Resources.register();
		blocks.register();
		blockEntityTypes.register();
		items.register();
		Packets.INSTANCE.register();

		peripherals.put(
			new Identifier(MOD_ID,"core"),
			CorePeriph::new
		);
		peripherals.put(
				new Identifier(MOD_ID,"net"),
				InternetPeriph::new
		);

		LOGGER.info("Hello Fabric world!");
	}
}