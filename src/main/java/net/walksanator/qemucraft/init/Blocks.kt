package net.walksanator.qemucraft.init

import net.minecraft.block.AbstractBlock
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier
import net.walksanator.qemucraft.QemuCraft.MOD_ID
import net.walksanator.qemucraft.blocks.TerminalBlock


class Blocks {

    val terminal = TerminalBlock(AbstractBlock.Settings.create());
    fun register() {
        Registry.register(Registries.BLOCK, Identifier(MOD_ID,"terminal_block"),terminal)
    }
}
