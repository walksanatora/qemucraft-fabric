package net.walksanator.qemucraft.peripherals

import net.minecraft.nbt.NbtCompound
import net.minecraft.util.Identifier
import net.walksanator.qemucraft.Peripheral
import net.walksanator.qemucraft.QemuCraft.MOD_ID
import net.walksanator.qemucraft.QemuFunction
import net.walksanator.qemucraft.blocks.TerminalEntity

class InternetPeriph(val blockEntity: TerminalEntity, val data: NbtCompound) : Peripheral {

    override fun getQemuArgs(): List<String> = listOf("-netdev", "user,id=net0", "-device", "virtio-net-device,netdev=net0")


    override fun save(): NbtCompound = NbtCompound()

    override fun canInstall(others: List<Peripheral>): Boolean = others.filterIsInstance<InternetPeriph>().isEmpty()
    override fun getId(): Identifier = Identifier(MOD_ID,"net")


    override fun getFunctions(): List<QemuFunction> = listOf()
}