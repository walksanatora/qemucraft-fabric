package net.walksanator.qemucraft.init

import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier
import net.walksanator.qemucraft.QemuCraft.MOD_ID
import net.walksanator.qemucraft.blocks.TerminalEntity

class Items(blocks: Blocks) {

    val terminal = BlockItem(blocks.terminal, Item.Settings())

    fun register() {
        Registry.register(Registries.ITEM, Identifier(MOD_ID,"terminal_block"),terminal)
    }
}