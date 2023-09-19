package net.walksanator.qemucraft;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

import java.util.List;

public interface Peripheral {

    /**
     * returns a list of args to pass to qemu to eg: mount drives/connect network
     * @return A list of args to pass into qemu
     */
    List<String> getQemuArgs();

    /**
     * save the state of the peripheral to NBT
     * @return persistent data to store along side the periperal
     */
    NbtCompound save();

    /**
     * checks if a peripheral can be installed into a machine
     * @param others a list of other peripherals already connected to the machine
     * @return if the peripheral can be installed
     */
    boolean canInstall(List<Peripheral> others);

    Identifier getId();

    /**
     * get a list of HLAPI functions
     * @return a list of pairs of input byte size and a function that takes a bytebuffer(input) and returns a bytebuffer(output)
     */
    List<QemuFunction> getFunctions();
}
