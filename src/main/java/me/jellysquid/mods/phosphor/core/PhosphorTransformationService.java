package me.jellysquid.mods.phosphor.core;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.IncompatibleEnvironmentException;
import org.spongepowered.asm.mixin.Mixins;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
public class PhosphorTransformationService implements ITransformationService {
    @Nonnull
    @Override
    public String name() {
        return "phosphor";
    }

    @Override
    public void initialize(IEnvironment environment) {
    }

    @Override
    public void beginScanning(IEnvironment environment) {

    }

    @Override
    public void onLoad(IEnvironment env, Set<String> otherServices) throws IncompatibleEnvironmentException {
        Mixins.addConfiguration("phosphor.mixins.json");
    }

    @Nonnull
    @Override
    public List<ITransformer> transformers() {
        return Collections.emptyList();
    }
}
