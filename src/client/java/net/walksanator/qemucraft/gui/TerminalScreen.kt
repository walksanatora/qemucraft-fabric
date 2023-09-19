package net.walksanator.qemucraft.gui

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.platform.TextureUtil
import com.mojang.blaze3d.systems.RenderSystem
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.Framebuffer
import net.minecraft.client.gl.SimpleFramebuffer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.ButtonWidget.Builder
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.render.BufferRenderer
import net.minecraft.client.render.VertexFormat.DrawMode
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.text.Text
import net.minecraft.util.math.Vec3d
import net.walksanator.qemucraft.Shaders
import net.walksanator.qemucraft.blocks.TerminalEntity
import net.walksanator.qemucraft.init.Packets
import net.walksanator.qemucraft.util.math.Mat4
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.*
import org.lwjgl.opengl.GL11.GL_FLOAT
import org.lwjgl.opengl.GL11.GL_TRIANGLES
import kotlin.experimental.xor
import kotlin.math.round

private val buf = BufferUtils.createByteBuffer(16384)

private val vbo = GL30.glGenBuffers()
private val vao = GL30.glGenVertexArrays()
private val screenTex = createTexture()
private val charsetTex = createTexture()

class TerminalScreen(val te: TerminalEntity) : Screen(Text.translatable("block.retrocomputers.terminal")) {
    private val dedup_chars = "`-=;',./[]\\1234567890QWERTYUIOPASDFGHJKLZXCVBNM".toList()
    private var uMvp = 0
    private var uCharset = 0
    private var uScreen = 0
    private var aXyz = 0
    private var aUv = 0
    private lateinit var onoff: ClickableWidget

    private var fb: Framebuffer? = null

    override fun tick() {
        val minecraft = client ?: return
        val dist = minecraft.player?.getCameraPosVec(1f)?.squaredDistanceTo(Vec3d.ofCenter(te.pos))
            ?: Double.POSITIVE_INFINITY
        if (dist > 10 * 10) minecraft.setScreen(null)
    }

    override fun render(context: DrawContext?, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        renderBackground(context)

        render(context!!.matrices, mouseX, mouseY, delta)
    }

     private fun render(matrices: MatrixStack, mouseX: Int, mouseY: Int, delta: Float) {

        val sh = Shaders.screen()
        val fb = fb ?: return
        val mc = client ?: return

        fb.setTexFilter(if ((mc.window.scaleFactor.toInt() % 2) == 0) GL11.GL_NEAREST else GL11.GL_LINEAR)

        fb.beginWrite(true)
        val mat = Mat4.ortho(0.0f, 1.0f, 1.0f, 0.0f, -1.0f, 1.0f)

        GL30.glUseProgram(sh)
        GL30.glBindVertexArray(vao)
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, vbo)

        GL20.glEnableVertexAttribArray(aXyz)
        GL20.glEnableVertexAttribArray(aUv)

        RenderSystem.activeTexture(GL13.GL_TEXTURE0)
        //RenderSystem.enableTexture()
        RenderSystem.bindTexture(screenTex)

        buf.clear()
        val fbuf = buf.asFloatBuffer()
        mat.intoBuffer(fbuf)
        fbuf.flip()
        GL30.glUniformMatrix4fv(uMvp, false, fbuf)

        GL30.glUniform1i(uScreen, 0)

        buf.clear()
        buf.put(te.screen)

        if (te.cm == 1 || (te.cm == 2 && (System.currentTimeMillis() / 500) % 2 == 0L)) {
            val ci = te.cx + te.cy * 80
            buf.put(ci, te.screen[ci] xor 0x80.toByte())
        }

        buf.rewind()
        GlStateManager._texImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_R16I, 80, 60, 0, GL30.GL_RED_INTEGER, GL11.GL_UNSIGNED_BYTE, buf.asIntBuffer())

        RenderSystem.activeTexture(GL13.GL_TEXTURE2)
        //RenderSystem.enableTexture()
        RenderSystem.bindTexture(charsetTex)
        GL30.glUniform1i(uCharset, 2)

        buf.clear()
        buf.put(te.charset)
        buf.rewind()
        GlStateManager._texImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_R16I, 8, 256, 0, GL30.GL_RED_INTEGER, GL11.GL_UNSIGNED_BYTE, buf.asIntBuffer())

        GL11.glDrawArrays(GL_TRIANGLES, 0, 6)

        GL20.glDisableVertexAttribArray(aXyz)
        GL20.glDisableVertexAttribArray(aUv)

        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, 0)
        GL30.glBindVertexArray(0)
        GL30.glUseProgram(0)

        RenderSystem.bindTexture(0)
        //RenderSystem.disableTexture()
        RenderSystem.activeTexture(GL13.GL_TEXTURE0)
        RenderSystem.bindTexture(0)

        mc.framebuffer.beginWrite(true)

        val swidth = 8 * 80 * 0.5
        val sheight = 8 * 50 * 0.5
        val x1 = round(width / 2.0 - swidth / 2.0)
        val y1 = round(height / 2.0 - sheight / 2.0)

        matrices.push()
        matrices.translate(x1, y1, -2000.0) // why the -2000? not sure

        val shader = mc.gameRenderer.blitScreenProgram
        shader.addSampler("DiffuseSampler", fb.colorAttachment)
        shader.modelViewMat?.set(matrices.peek().positionMatrix)
        shader.projectionMat?.set(RenderSystem.getProjectionMatrix())
        shader.bind()

        val t = RenderSystem.renderThreadTesselator()
        val buf = t.buffer
        buf.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR)
        buf.vertex(0.0, 0.0, 0.0).texture(0f, 1f).color(255, 255, 255, 255).next()
        buf.vertex(0.0, sheight, 0.0).texture(0f, 0f).color(255, 255, 255, 255).next()
        buf.vertex(swidth, sheight, 0.0).texture(1f, 0f).color(255, 255, 255, 255).next()
        buf.vertex(swidth, 0.0, 0.0).texture(1f, 1f).color(255, 255, 255, 255).next()
        BufferRenderer.draw(buf.end())

        shader.unbind()

        matrices.pop()
    }

    override fun keyPressed(key: Int, scancode: Int, modifiers: Int): Boolean {
        if (super.keyPressed(key, scancode, modifiers)) return true
        val char = key.toChar()
        if (
            dedup_chars.contains(char)
            ) return true
        pushKey(char)
        return true
    }

    override fun charTyped(c: Char, modifiers: Int): Boolean {
        if (super.charTyped(c, modifiers)) return true

        val result: Char? = when (c) {
            in '\u0001'..'\u007F' -> c
            else -> null
        }

        if (result != null) pushKey(result)

        return result != null
    }

    private fun pushKey(c: Char) {
        val buffer = PacketByteBuf(Unpooled.buffer())
        buffer.writeBlockPos(te.pos)
        buffer.writeChar(c.toInt())
        ClientPlayNetworking.send(Packets.Server.TERMINAL_KEY_TYPED, buffer)
    }

    override fun init() {
        //client!!.keyboard.setRepeatEvents(true)
        onoff = ButtonNoKeyboardWidget
            .Builder(Text.literal("Power")) { btn -> btn.message = Text.literal(
                if (btn.message.string.equals("Power")) "Shutdown" else "Power")
                println("CLICKED")
            }
                .dimensions(0,0,100,20).build()
        addDrawableChild(onoff)
        initDrawData()
        initFb()
    }

    private fun initDrawData() {
        val sh = Shaders.screen()

        GL30.glUseProgram(sh)
        GL30.glBindVertexArray(vao)
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, vbo)

        uMvp = GL30.glGetUniformLocation(sh, "mvp")
        uCharset = GL30.glGetUniformLocation(sh, "charset")
        uScreen = GL30.glGetUniformLocation(sh, "screen")

        aXyz = GL30.glGetAttribLocation(sh, "xyz")
        aUv = GL30.glGetAttribLocation(sh, "uv")

        GL20.glVertexAttribPointer(aXyz, 3, GL_FLOAT, false, 20, 0)
        GL20.glVertexAttribPointer(aUv, 2, GL_FLOAT, false, 20, 12)

        buf.clear()

        floatArrayOf(
            0f, 0f, 0f, 0f, 0f,
            1f, 1f, 0f, 1f, 1f,
            1f, 0f, 0f, 1f, 0f,

            0f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 1f,
            1f, 1f, 0f, 1f, 1f
        ).forEach { buf.putFloat(it) }

        buf.rewind()

        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, buf, GL15.GL_STATIC_DRAW)

        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, 0)
        GL30.glBindVertexArray(0)
        GL30.glUseProgram(0)
    }

    private fun initFb() {
        fb?.delete()
        val scale = 4
        fb = SimpleFramebuffer(80 * 8 * scale, 50 * 8 * scale, false, MinecraftClient.IS_SYSTEM_MAC)
    }

    override fun removed() {
        //client!!.keyboard.setRepeatEvents(false)
        fb?.delete()
        fb = null
    }

    override fun shouldPause() = false

}

class ButtonNoKeyboardWidget(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    msg: Text,
    pa: PressAction,
    nar: NarrationSupplier
) : ButtonWidget(x, y, width, height, msg, pa, nar) {
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        return false
    }

    override fun isFocused(): Boolean = false

    class Builder(private val message: Text, private val onPress: PressAction) {
        private var tooltip: Tooltip? = null
        private var x = 0
        private var y = 0
        private var width = 150
        private var height = 20
        private var narrationSupplier: NarrationSupplier

        init {
            narrationSupplier = DEFAULT_NARRATION_SUPPLIER
        }

        fun position(x: Int, y: Int): Builder {
            this.x = x
            this.y = y
            return this
        }

        fun width(width: Int): Builder {
            this.width = width
            return this
        }

        fun size(width: Int, height: Int): Builder {
            this.width = width
            this.height = height
            return this
        }

        fun dimensions(x: Int, y: Int, width: Int, height: Int): Builder {
            return position(x, y).size(width, height)
        }

        fun tooltip(tooltip: Tooltip?): Builder {
            this.tooltip = tooltip
            return this
        }

        fun narrationSupplier(narrationSupplier: NarrationSupplier): Builder {
            this.narrationSupplier = narrationSupplier
            return this
        }

        fun build(): ButtonNoKeyboardWidget {
            val buttonWidget = ButtonNoKeyboardWidget(
                x,
                y, width, height, message, onPress, narrationSupplier
            )
            buttonWidget.tooltip = tooltip
            return buttonWidget
        }
    }


}

private fun createTexture(): Int {
    val tex = TextureUtil.generateTextureId()
    RenderSystem.bindTexture(tex)
    RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
    RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)
    RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT)
    RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT)
    RenderSystem.bindTexture(0)
    return tex
}