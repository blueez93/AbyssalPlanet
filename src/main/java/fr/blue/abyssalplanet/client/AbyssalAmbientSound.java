package fr.blue.abyssalplanet.client;

import fr.blue.abyssalplanet.registry.ModDimensions;
import fr.blue.abyssalplanet.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public class AbyssalAmbientSound extends AbstractTickableSoundInstance {
    public AbyssalAmbientSound() {
        super(ModSounds.ABYSSAL_THEME.get(), SoundSource.MUSIC, RandomSource.create());
        this.looping = true;
        this.delay = 0;
        this.volume = 0.65F;
        this.pitch = 1.0F;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    @Override
    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || !minecraft.player.level().dimension().equals(ModDimensions.ABYSSAL_PLANET_LEVEL)) {
            stop();
        }
    }

    public void stopImmediately() {
        stop();
    }
}
