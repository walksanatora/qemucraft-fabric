package net.walksanator.qemucraft;

import java.nio.ByteBuffer;

public interface QemuFunction {
    /**
     *
     * @return the number of bytes taken as input
     */
    int getInputSize();

    /**
     * runs the Qemu function
     * @param input a ByteBuffer of size `getInputSize`
     * @return a bytebuffer of outputs which will get appended to the "sending buffer"
     */
    ByteBuffer execute(ByteBuffer input);
}
