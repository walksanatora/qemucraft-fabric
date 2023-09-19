package net.walksanator.qemucraft.blocks

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.ActionResult
import net.minecraft.util.ActionResult.FAIL
import net.minecraft.util.ActionResult.SUCCESS
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.walksanator.qemucraft.Peripheral
import net.walksanator.qemucraft.QemuCraft
import net.walksanator.qemucraft.init.Packets
import net.walksanator.qemucraft.util.unsigned
import org.lwjgl.glfw.GLFW
import kotlin.experimental.xor

class TerminalBlock(settings: Settings) : BlockWithEntity(settings), BlockEntityTicker<TerminalEntity> {

    override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, hit: BlockHitResult): ActionResult {
        if (world.getBlockEntity(pos) !is TerminalEntity) return FAIL
        if (!world.isClient()) {
            val pbb = PacketByteBufs.create()
            pbb.writeBlockPos(pos)
            ServerPlayNetworking.send(player as ServerPlayerEntity?, Packets.Client.OPEN_TERMINAL,pbb)
        }
        return SUCCESS
    }

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return TerminalEntity(pos,state)
    }

    override fun tick(world: World, pos: BlockPos, state: BlockState, data: TerminalEntity) {
        var error = false

        when (data.command.unsigned) {
            1 -> data.getIndices(data.bx2, data.by2, data.bw, data.bh).forEach { data.screen[it] = data.bx1.toByte() }
            2 -> data.getIndices(data.bx2, data.by2, data.bw, data.bh).forEach { data.screen[it] = data.screen[it] xor 0x80.toByte() }
            3 -> data.getIndices(data.bx2, data.by2, data.bw, data.bh).zip(data.getIndices(data.bx1, data.by1, data.bw, data.bh)).forEach { (dest, src) -> data.screen[dest] = data.screen[src] }
            4 -> QemuCraft.resources!!.charset().copyInto(data.charset)
            255 -> Unit
            else -> error = true
        }

        if (data.command in 1..4) world.updateListeners(pos, state, state, 3)

        data.command = if (error) -1 else 0
    }
}

class TerminalEntity(pos: BlockPos, state: BlockState) : BlockEntity(QemuCraft.blockEntityTypes.terminal, pos, state) {

    val attached: MutableList<Peripheral> = ArrayList()

    val screen = ByteArray(80 * 25) { 0x20 }
    val charset = QemuCraft.resources!!.charset.clone()
    val kb = ByteArray(16)

    var command: Byte = 0

    var row = 0
    var cx = 0
    var cy = 0
    var cm = 2
    var kbs = 0
    var kbp = 0

    var bx1 = 0
    var by1 = 0
    var bx2 = 0
    var by2 = 0
    var bw = 0
    var bh = 0

    var char = 0

    fun pushKey(char: Char): Boolean {
        val byte: Byte = when (char) {
            in '\u0001'..'\u007F' -> char.code.toByte() // lower ascii characters
            else -> when (char.toInt()) { //remap some GL charachters to our font
                GLFW.GLFW_KEY_BACKSPACE -> 0x08
                GLFW.GLFW_KEY_ENTER -> 0x0D
                GLFW.GLFW_KEY_HOME -> 0x80
                GLFW.GLFW_KEY_END -> 0x81
                GLFW.GLFW_KEY_UP -> 0x82
                GLFW.GLFW_KEY_DOWN -> 0x83
                GLFW.GLFW_KEY_LEFT -> 0x84
                GLFW.GLFW_KEY_RIGHT -> 0x85
                GLFW.GLFW_KEY_TAB -> 0x09
                else -> null
            }?.toByte()
        } ?: return false

        //QemuCraft.LOGGER.info("char: %s byte: %s".format(char,byte))

        screen[1*80+0] = byte
        var idx = 0
        for (char in "kid: %s".format(byte.toString())) {
            screen[1*80+3+idx] = char.toByte()
            idx++
        }
        for (i in 0 until 5) {
            screen[1*80+3+idx]  = 0x20
            idx++
        }

        for (char in 0 until 256) {
            val x = char%16
            val y = char.floorDiv(16)
            screen[(2+y)*80+x] = char.toByte()
        }

        val ret = if ((kbp + 1) % 16 != kbs) {
            kb[kbp] = byte
            kbp = (kbp + 1) % 16
            true
        } else false
        world?.updateListeners(pos,cachedState,cachedState,Block.NOTIFY_LISTENERS)
        return ret
    }

    fun getIndices(x1: Int, y1: Int, w: Int, h: Int): Sequence<Int> = sequence {
        for (i in 0 until h) for (j in 0 until w) {
            val x = j + x1
            val y = i + y1

            if (x in 0 until 80 && y in 0 until 60)
                yield(x + 80 * y)
        }
    }

    override fun toUpdatePacket(): Packet<ClientPlayPacketListener>? {
        return BlockEntityUpdateS2CPacket.create(this)
    }


     fun readData(at: Byte): Byte {
        return when (val off = at.unsigned) {
            0x00 -> row.toByte()
            0x01 -> cx.toByte()
            0x02 -> cy.toByte()
            0x03 -> cm.toByte()
            0x04 -> kbs.toByte()
            0x05 -> kbp.toByte()
            0x06 -> kb[kbs]
            0x07 -> command
            0x08 -> bx1.toByte()
            0x09 -> by1.toByte()
            0x0A -> bx2.toByte()
            0x0B -> by2.toByte()
            0x0C -> bw.toByte()
            0x0D -> bh.toByte()
            0x0E -> char.toByte()
            in 0x10..0x5F -> screen[row * 80 + off - 0x10]
            in 0x60..0x67 -> charset[char * 8 + off - 0x60]
            else -> 0
        }
    }

     fun storeData(at: Byte, data: Byte) {
        when (val off = at.unsigned) {
            0x00 -> row = data.unsigned % 25
            0x01 -> cx = data.unsigned % 80
            0x02 -> cy = data.unsigned % 25
            0x03 -> cm = data.unsigned % 3
            0x04 -> kbs = data.unsigned % 16
            0x05 -> kbp = data.unsigned % 16
            0x06 -> kb[kbs] = data
            0x07 -> command = data
            0x08 -> bx1 = data.unsigned % 80
            0x09 -> by1 = data.unsigned % 25
            0x0A -> bx2 = data.unsigned % 80
            0x0B -> by2 = data.unsigned % 25
            0x0C -> bw = data.unsigned
            0x0D -> bh = data.unsigned
            0x0E -> char = data.unsigned
            in 0x10..0x5F -> screen[row * 80 + off - 0x10] = data
            in 0x60..0x67 -> charset[char * 8 + off - 0x60] = data
        }

        val needsClientUpdate = at.unsigned in setOf(0x01, 0x02, 0x03) + (0x10..0x67)
        if (needsClientUpdate)
            getWorld()?.updateListeners(getPos(), cachedState, cachedState, 3)
        markDirty()
    }

    override fun toInitialChunkDataNbt(): NbtCompound {
        val tag = super.toInitialChunkDataNbt()
        // these are big, TODO: only send changed data
        tag.putByteArray("screen", screen)
        tag.putByteArray("charset", charset)
        tag.putByte("cx", cx.toByte())
        tag.putByte("cy", cy.toByte())
        tag.putByte("cm", cm.toByte())
        return tag
    }

    override fun writeNbt(tag: NbtCompound) {
        super.writeNbt(tag)
        tag.putByteArray("screen", screen)
        tag.putByteArray("charset", charset)
        tag.putByteArray("kb", kb)
        tag.putByte("command", command)
        tag.putByte("row", row.toByte())
        tag.putByte("cx", cx.toByte())
        tag.putByte("cy", cy.toByte())
        tag.putByte("cm", cm.toByte())
        tag.putByte("kbs", kbs.toByte())
        tag.putByte("kbp", kbp.toByte())
        tag.putByte("bx1", bx1.toByte())
        tag.putByte("by1", by1.toByte())
        tag.putByte("bx2", bx2.toByte())
        tag.putByte("by2", by2.toByte())
        tag.putByte("bw", bw.toByte())
        tag.putByte("bh", bh.toByte())
        tag.putByte("char", char.toByte())
    }

    override fun readNbt(tag: NbtCompound) {
        super.readNbt(tag)
        val world = getWorld()

        tag.getByteArray("screen").copyInto(screen)
        tag.getByteArray("charset").copyInto(charset)
        cx = tag.getByte("cx").unsigned
        cy = tag.getByte("cy").unsigned
        cm = tag.getByte("cm").unsigned

        if (world == null || !world.isClient) {
            tag.getByteArray("screen").copyInto(screen)
            tag.getByteArray("charset").copyInto(charset)
            tag.getByteArray("kb").copyInto(kb)
            command = tag.getByte("command")
            row = tag.getByte("row").unsigned
            kbs = tag.getByte("kbs").unsigned
            kbp = tag.getByte("kbp").unsigned
            bx1 = tag.getByte("bx1").unsigned
            by1 = tag.getByte("by1").unsigned
            bx2 = tag.getByte("bx2").unsigned
            by2 = tag.getByte("by2").unsigned
            bw = tag.getByte("bw").unsigned
            bh = tag.getByte("bh").unsigned
            char = tag.getByte("char").unsigned
        }
    }
}