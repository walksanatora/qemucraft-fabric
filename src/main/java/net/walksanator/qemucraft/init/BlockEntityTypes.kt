package net.walksanator.qemucraft.init

import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier
import net.walksanator.qemucraft.QemuCraft.MOD_ID
import net.walksanator.qemucraft.blocks.TerminalEntity

class BlockEntityTypes(blocks: Blocks) {

    val terminal = FabricBlockEntityTypeBuilder.create(::TerminalEntity,blocks.terminal).build()

    fun register() {
        Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier(MOD_ID,"terminal_block_entity"),terminal)
    }
}