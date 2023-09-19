package net.walksanator.qemucraft.peripherals

import net.minecraft.nbt.NbtCompound
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.walksanator.qemucraft.Peripheral
import net.walksanator.qemucraft.QemuCraft
import net.walksanator.qemucraft.QemuCraft.MOD_ID
import net.walksanator.qemucraft.QemuFunction
import net.walksanator.qemucraft.blocks.TerminalEntity
import java.nio.ByteBuffer
import kotlin.math.min

class CorePeriph(val blockEntity: TerminalEntity, val data: NbtCompound) : Peripheral {

    override fun getQemuArgs(): List<String> = listOf()

    override fun save(): NbtCompound = NbtCompound()
    override fun canInstall(others: MutableList<Peripheral>): Boolean = others.filterIsInstance<CorePeriph>().isEmpty()
    override fun getId(): Identifier = Identifier(MOD_ID,"core")

    override fun getFunctions(): List<QemuFunction> = listOf(
        object : QemuFunction { // get Peripheral count () -> (byte)
            override fun getInputSize(): Int = 0
            override fun execute(input: ByteBuffer?): ByteBuffer = ByteBuffer.allocate(1).put(blockEntity.attached.size.toByte())
        },
        object : QemuFunction { // get peripheral id (byte) -> (byte,char...)
            override fun getInputSize(): Int = 1
            override fun execute(input: ByteBuffer): ByteBuffer {
                val buf = ByteBuffer.allocate(256)
                val target = blockEntity.attached[input.get().toInt()]
                val name = target.id.toString()
                val size = min(name.length,255).toByte()
                buf.put(size)
                for (char in 0 until size) {
                        buf.put(name[char].toByte())
                }
                return buf
            }
        },
        object : QemuFunction { // read value from Terminal memory (byte) -> (byte)
            override fun getInputSize(): Int = 1
            override fun execute(input: ByteBuffer): ByteBuffer = ByteBuffer.allocate(1).put(blockEntity.readData(input.get()))
        },
        object : QemuFunction { // write value to terminal memory (byte,byte) -> ()
            override fun getInputSize(): Int = 2
            override fun execute(input: ByteBuffer): ByteBuffer {
                blockEntity.storeData(input.get(),input.get())
                return ByteBuffer.allocate(0)
            }
        }
    )
}