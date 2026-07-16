package fr.blue.abyssalplanet.registry;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.fluid.AbyssalWaterFluidType;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, AbyssalPlanet.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(ForgeRegistries.FLUIDS, AbyssalPlanet.MOD_ID);

    public static final RegistryObject<FluidType> ABYSSAL_WATER_TYPE =
            FLUID_TYPES.register("abyssal_water", AbyssalWaterFluidType::new);

    public static final RegistryObject<FlowingFluid> ABYSSAL_WATER_SOURCE =
            FLUIDS.register("abyssal_water", () -> new ForgeFlowingFluid.Source(properties()));

    public static final RegistryObject<FlowingFluid> FLOWING_ABYSSAL_WATER =
            FLUIDS.register("flowing_abyssal_water", () -> new ForgeFlowingFluid.Flowing(properties()));

    private ModFluids() {
    }

    private static ForgeFlowingFluid.Properties properties() {
        return new ForgeFlowingFluid.Properties(
                ABYSSAL_WATER_TYPE,
                ABYSSAL_WATER_SOURCE,
                FLOWING_ABYSSAL_WATER
        ).block(ModBlocks.ABYSSAL_WATER)
                .bucket(ModItems.ABYSSAL_WATER_BUCKET)
                .slopeFindDistance(4)
                .levelDecreasePerBlock(1)
                .tickRate(5)
                .explosionResistance(100.0F);
    }

    public static boolean isAbyssalWater(FluidState state) {
        Fluid fluid = state.getType();
        return fluid == ABYSSAL_WATER_SOURCE.get() || fluid == FLOWING_ABYSSAL_WATER.get();
    }

    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
        FLUIDS.register(eventBus);
    }
}
