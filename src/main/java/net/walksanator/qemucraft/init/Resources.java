package net.walksanator.qemucraft.init;

import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.walksanator.qemucraft.QemuCraft;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static net.walksanator.qemucraft.QemuCraft.MOD_ID;

public record Resources(
    byte[] charset
) {
    private static final Identifier ID = new Identifier(MOD_ID, "data");

    public static void register() {
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new SimpleResourceReloadListener<Resources>() {
            @Override
            public CompletableFuture<Resources> load(ResourceManager manager, Profiler profiler, Executor executor) {
                return CompletableFuture.supplyAsync(() -> {
                    byte[] charset = loadImage(manager, "charset.bin");

                    return new Resources(charset);
                }, executor);
            }

            @Override
            public CompletableFuture<Void> apply(Resources data, ResourceManager manager, Profiler profiler, Executor executor) {
                return CompletableFuture.runAsync(() -> {
                    QemuCraft.resources = data;
                }, executor);
            }

            @Override
            public Identifier getFabricId() {
                return ID;
            }
        });
    }

    private static byte[] loadImage(ResourceManager manager, String name) {
        Optional<Resource> res = manager.getResource(new Identifier(MOD_ID, name));
        if (res.isPresent()) {
            try {
                return res.get().getInputStream().readAllBytes();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return new byte[0];
    }
}
