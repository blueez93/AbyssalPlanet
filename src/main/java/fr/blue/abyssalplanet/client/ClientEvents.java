package fr.blue.abyssalplanet.client;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.registry.ModEntities;
import fr.blue.abyssalplanet.registry.ModBlockEntities;
import fr.blue.abyssalplanet.registry.ModFluids;
import fr.blue.abyssalplanet.item.ZwoingItem;
import fr.blue.abyssalplanet.registry.ModItems;
import fr.blue.abyssalplanet.registry.ModMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = AbyssalPlanet.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public class ClientEvents {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.ABYSSAL_POUCH.get(), AbyssalPouchScreen::new);
            ItemBlockRenderTypes.setRenderLayer(ModFluids.ABYSSAL_WATER_SOURCE.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModFluids.FLOWING_ABYSSAL_WATER.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(fr.blue.abyssalplanet.registry.ModBlocks.ABYSSAL_EGG.get(), RenderType.cutout());
        });
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(
                ModEntities.ABYSSAL_TYPHOON.get(),
                AbyssalTyphoonRenderer::new
        );
        event.registerEntityRenderer(
                ModEntities.ABYSSAL_OCTOPUS.get(),
                AbyssalOctopusRenderer::new
        );
        event.registerEntityRenderer(
                ModEntities.ABYSSAL_INK_PUDDLE.get(),
                NoopRenderer::new
        );
        event.registerEntityRenderer(
                ModEntities.LUMINOUS_ABYSSAL_FISH.get(),
                LuminousAbyssalFishRenderer::new
        );
        event.registerEntityRenderer(
                ModEntities.ABYSSAL_BLUE_MOONFISH.get(),
                AbyssalBlueMoonfishRenderer::new
        );
        event.registerEntityRenderer(
                ModEntities.MINI_KRAKEN.get(),
                MiniKrakenRenderer::new
        );
        event.registerEntityRenderer(
                ModEntities.BABY_KRAKEN.get(),
                BabyKrakenRenderer::new
        );
        event.registerEntityRenderer(
                ModEntities.BABY_KRAKEN_INK_BALL.get(),
                NoopRenderer::new
        );
        event.registerEntityRenderer(
                ModEntities.BABY_KRAKEN_BUBBLE.get(),
                NoopRenderer::new
        );
        event.registerEntityRenderer(
                ModEntities.KRAKEN_BOSS.get(),
                KrakenBossRenderer::new
        );
        event.registerEntityRenderer(
                ModEntities.KRAKEN_TENTACLE.get(),
                KrakenTentacleRenderer::new
        );
        event.registerEntityRenderer(
                ModEntities.KRAKEN_INK_BALL.get(),
                NoopRenderer::new
        );
        event.registerEntityRenderer(
                ModEntities.PLAYER_ABYSSAL_INK_BALL.get(),
                NoopRenderer::new
        );
        event.registerEntityRenderer(
                ModEntities.ABYSSAL_PORTAL.get(),
                AbyssalPortalRenderer::new
        );
        event.registerEntityRenderer(
                ModEntities.ABYSSAL_POUCH_GHOST.get(),
                AbyssalPouchGhostRenderer::new
        );
        event.registerEntityRenderer(
                ModEntities.ZWOING.get(),
                ZwoingRenderer::new
        );
        event.registerEntityRenderer(
                ModEntities.ZWOING_PROJECTILE.get(),
                context -> new ThrownItemRenderer<>(context, 0.6F, true)
        );
        event.registerEntityRenderer(
                ModEntities.ABYSSAL_VIPER.get(),
                AbyssalViperRenderer::new
        );
        event.registerEntityRenderer(
                ModEntities.ABYSSAL_SERPENT.get(),
                AbyssalSerpentRenderer::new
        );
        event.registerEntityRenderer(
                ModEntities.GEORGES_BRIOCHARD.get(),
                GeorgesBriochardRenderer::new
        );
        event.registerEntityRenderer(ModEntities.GEORGES_JUNIOR.get(), GeorgesJuniorRenderer::new);
        event.registerEntityRenderer(ModEntities.GEORGES_SENIOR.get(), GeorgesSeniorRenderer::new);
        event.registerEntityRenderer(ModEntities.ABYSSAL_SHRIMP.get(), AbyssalShrimpRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ABYSSAL_EGG.get(), context -> new AbyssalEggRenderer());
        event.registerBlockEntityRenderer(ModBlockEntities.GEORGES_HEAD.get(), context -> new GeorgesHeadBlockRenderer());
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ZwoingModel.LAYER_LOCATION, ZwoingModel::createBodyLayer);
        event.registerLayerDefinition(AbyssalBlueMoonfishModel.LAYER_LOCATION, AbyssalBlueMoonfishModel::createBodyLayer);
        event.registerLayerDefinition(LuminousAbyssalFishModel.LAYER_LOCATION, LuminousAbyssalFishModel::createBodyLayer);
        event.registerLayerDefinition(AbyssalViperModel.LAYER_LOCATION, AbyssalViperModel::createBodyLayer);
        event.registerLayerDefinition(AbyssalSerpentModel.LAYER_LOCATION, AbyssalSerpentModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(
                (stack, tintIndex) -> tintIndex == 0 ? ZwoingItem.getVariantTint(stack) : 0xFFFFFF,
                ModItems.ZWOING.get()
        );
    }
}
