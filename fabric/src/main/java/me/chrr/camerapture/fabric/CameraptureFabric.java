package me.chrr.camerapture.fabric;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.DownloadQueue;
import me.chrr.camerapture.config.Config;
import me.chrr.camerapture.config.SyncedConfig;
import me.chrr.camerapture.entity.PictureFrameEntity;
import me.chrr.camerapture.item.AlbumItem;
import me.chrr.camerapture.item.CameraItem;
import me.chrr.camerapture.item.PictureItem;
import me.chrr.camerapture.net.clientbound.DownloadPartialPicturePacket;
import me.chrr.camerapture.net.clientbound.PictureErrorPacket;
import me.chrr.camerapture.net.clientbound.RequestUploadPacket;
import me.chrr.camerapture.net.serverbound.NewPicturePacket;
import me.chrr.camerapture.net.serverbound.RequestDownloadPacket;
import me.chrr.camerapture.net.clientbound.SyncConfigPacket;
import me.chrr.camerapture.net.serverbound.UploadPartialPicturePacket;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import me.chrr.camerapture.registry.ValuationCommands;
import me.chrr.camerapture.registry.ValuationRuntime;
import net.minecraft.resource.ResourceType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.stat.StatFormatter;
import net.minecraft.stat.Stats;

public class CameraptureFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Camerapture.CONFIG_MANAGER.load();

        this.registerContent();
        this.registerPackets();
        this.registerEvents();
        ResourceManagerHelper.get(ResourceType.SERVER_DATA)
                .registerReloadListener(new FabricValuationReloadListener());
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                ValuationCommands.register(dispatcher));
    }

    public void registerContent() {
        // Camera
        Registry.register(Registries.ITEM, CameraItem.KEY, Camerapture.CAMERA);
        Registry.register(Registries.SOUND_EVENT, Camerapture.CAMERA_SHUTTER.getId(), Camerapture.CAMERA_SHUTTER);

        Registry.register(Registries.CUSTOM_STAT, "pictures_taken", Camerapture.PICTURES_TAKEN);
        Stats.CUSTOM.getOrCreateStat(Camerapture.PICTURES_TAKEN, StatFormatter.DEFAULT);

        // Picture
        Registry.register(Registries.ITEM, PictureItem.KEY, Camerapture.PICTURE);
        Registry.register(Registries.RECIPE_SERIALIZER, Camerapture.id("picture_cloning"), Camerapture.PICTURE_CLONING);

        // Album
        Registry.register(Registries.ITEM, AlbumItem.KEY, Camerapture.ALBUM);
        Registry.register(Registries.SCREEN_HANDLER, Camerapture.id("album"), Camerapture.ALBUM_SCREEN_HANDLER);
        Registry.register(Registries.SCREEN_HANDLER, Camerapture.id("album_lectern"), Camerapture.ALBUM_LECTERN_SCREEN_HANDLER);
        Registry.register(Registries.RECIPE_SERIALIZER, Camerapture.id("album_cloning"), Camerapture.ALBUM_CLONING);

        // Picture Frame
        Registry.register(Registries.ENTITY_TYPE, PictureFrameEntity.KEY, Camerapture.PICTURE_FRAME);
        Registry.register(Registries.SCREEN_HANDLER, Camerapture.id("picture_frame"), Camerapture.PICTURE_FRAME_SCREEN_HANDLER);

        // Data components
        Registry.register(Registries.DATA_COMPONENT_TYPE, Camerapture.id("picture_data"), Camerapture.PICTURE_DATA);
        Registry.register(Registries.DATA_COMPONENT_TYPE, Camerapture.id("camera_active"), Camerapture.CAMERA_ACTIVE);

        Registry.register(Registries.ITEM_GROUP, Camerapture.PHOTO_EXPEDITION_TAB_ID,
                Camerapture.createPhotoExpeditionTab());
    }

    public void registerPackets() {
        FabricNetworkAdapter networkAdapter = (FabricNetworkAdapter) Camerapture.NETWORK;

        networkAdapter.registerServerBound(NewPicturePacket.class, NewPicturePacket.NET_CODEC);
        networkAdapter.registerServerBound(RequestDownloadPacket.class, RequestDownloadPacket.NET_CODEC);
        networkAdapter.registerServerBound(UploadPartialPicturePacket.class, UploadPartialPicturePacket.NET_CODEC);
        networkAdapter.registerClientBound(PictureErrorPacket.class, PictureErrorPacket.NET_CODEC);
        networkAdapter.registerClientBound(RequestUploadPacket.class, RequestUploadPacket.NET_CODEC);
        networkAdapter.registerClientBound(SyncConfigPacket.class, SyncConfigPacket.NET_CODEC);
        networkAdapter.registerClientBound(DownloadPartialPicturePacket.class, DownloadPartialPicturePacket.NET_CODEC);

        Camerapture.registerPacketHandlers();
    }

    public void registerEvents() {
        // When a player joins, we send them our config.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            Config config = Camerapture.CONFIG_MANAGER.getConfig();
            Camerapture.NETWORK.sendToClient(handler.player, new SyncConfigPacket(SyncedConfig.fromServerConfig(config.server)));
        });

        // Run the download queue while the server is started.
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            DownloadQueue.getInstance().start(Camerapture.CONFIG_MANAGER.getConfig().server.msPerPicture);
            ValuationRuntime.start(server, FabricLoader.getInstance().getAllMods().stream()
                    .filter(container -> !container.getMetadata().getId().startsWith("generated_"))
                    .map(container -> container.getMetadata().getId() + "@"
                            + container.getMetadata().getVersion().getFriendlyString())
                    .toList());
        });
        ServerTickEvents.END_SERVER_TICK.register(ValuationRuntime::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            ValuationRuntime.stop(server);
            DownloadQueue.getInstance().stop();
        });
    }
}
