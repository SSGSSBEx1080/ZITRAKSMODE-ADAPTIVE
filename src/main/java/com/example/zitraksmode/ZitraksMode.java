package com.example.zitraksmode;

import com.example.zitraksmode.blocks.ModPaintings;
import com.example.zitraksmode.blocks.RandomArtPools;
import com.example.zitraksmode.caps.EnderCapability;
import com.example.zitraksmode.caps.PrinterRecipeCapability;
import com.example.zitraksmode.commands.IntimacyCommand;
import com.example.zitraksmode.commands.ScanBuildCommand;
import com.example.zitraksmode.criteria.AqualungDeepBreathTrigger;
import com.example.zitraksmode.criteria.AqualungDrownTrigger;
import com.example.zitraksmode.criteria.ChargedCreeperCastratedTrigger;
import com.example.zitraksmode.criteria.CreeperCastratedTrigger;
import com.example.zitraksmode.criteria.DeathNoteAllMethodsTrigger;
import com.example.zitraksmode.criteria.DildoSatLongTrigger;
import com.example.zitraksmode.criteria.EnderIntruderTrigger;
import com.example.zitraksmode.criteria.FindMahoragaOutpostTrigger;
import com.example.zitraksmode.criteria.GazanTradeTrigger;
import com.example.zitraksmode.criteria.HentaiLongStareTrigger;
import com.example.zitraksmode.criteria.HentaiMemeFacesTrigger;
import com.example.zitraksmode.criteria.IntruderFuckedTrigger;
import com.example.zitraksmode.menu.VacuumMenus;
import com.example.zitraksmode.network.VacuumNetwork;
import com.example.zitraksmode.network.LogiaNetwork;
import com.example.zitraksmode.util.LogiaGaugeCapability;
import com.example.zitraksmode.criteria.JBLFullVolumeTrigger;
import com.example.zitraksmode.criteria.KatanaDeflectKillTrigger;
import com.example.zitraksmode.criteria.KatanaParryTrigger;
import com.example.zitraksmode.criteria.KatanaUltimateTrigger;
import com.example.zitraksmode.criteria.KillAdunTrigger;
import com.example.zitraksmode.criteria.KoranFullyReadTrigger;
import com.example.zitraksmode.criteria.LabubuCollectorTrigger;
import com.example.zitraksmode.criteria.MahoragaKilledTrigger;
import com.example.zitraksmode.criteria.MahoragaSummonedTrigger;
import com.example.zitraksmode.criteria.MihActivateTrigger;
import com.example.zitraksmode.network.chigur.ChigurNetwork;
import com.example.zitraksmode.criteria.MilkBullTrigger;
import com.example.zitraksmode.criteria.MilkRavagerTrigger;
import com.example.zitraksmode.criteria.MinerDrainedTrigger;
import com.example.zitraksmode.criteria.PaintingPlacedTrigger;
import com.example.zitraksmode.network.scooter.ScooterPackets;
import com.example.zitraksmode.items.MaxMessengerItem;
import com.example.zitraksmode.recipe.ModRecipeSerializers;
import com.example.zitraksmode.max.MaxMessengerSavedData;
import com.example.zitraksmode.network.max.MaxPackets;
import com.example.zitraksmode.criteria.PrinterHammerTrigger;
import com.example.zitraksmode.network.MedusaNetwork;
import com.example.zitraksmode.criteria.PrinterHouseTrigger;
import com.example.zitraksmode.criteria.PrinterPrisonTrigger;
import com.example.zitraksmode.criteria.Secret1488Trigger;
import com.example.zitraksmode.criteria.SniperLongShotTrigger;
import com.example.zitraksmode.criteria.SteelBallGoldenRotationTrigger;
import com.example.zitraksmode.criteria.SteelBallKillTrigger;
import com.example.zitraksmode.criteria.SuperTNTActivatedTrigger;
import com.example.zitraksmode.criteria.TNTDildoBrokenTrigger;
import com.example.zitraksmode.criteria.TNTExplosionKillTrigger;
import com.example.zitraksmode.criteria.UniverseResetTrigger;
import com.example.zitraksmode.entities.AdunVillager;
import com.example.zitraksmode.entities.AntonChigurEntity;
import com.example.zitraksmode.entities.GazanEntity;
import com.example.zitraksmode.entities.MahoragaCinematicEntity;
import com.example.zitraksmode.entities.MahoragaEntity;
import com.example.zitraksmode.events.AdunDeathHandler;
import com.example.zitraksmode.events.CowMilkingHandler;
import com.example.zitraksmode.events.ModSpawnEvents;
import com.example.zitraksmode.events.SecretAchievementsHandler;
import com.example.zitraksmode.network.JBLSpeakerNetwork;
import com.example.zitraksmode.network.ModMessages;
import com.example.zitraksmode.network.ModPackets;
import com.example.zitraksmode.network.NetworkHandler;
import com.example.zitraksmode.network.PacketHandler;
import com.example.zitraksmode.network.SyncPrinterRecipeLibraryPacket;
import com.example.zitraksmode.recipe.ModRecipes;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import com.example.zitraksmode.villager.ModVillagers;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ZitraksMode.MODID)
public class ZitraksMode {

    public static final String MODID = "zitraksmode";
    public static final String KORAN = "koran";

    public static final Secret1488Trigger SECRET_1488 = CriteriaTriggers.register(new Secret1488Trigger());
    public static final com.example.zitraksmode.criteria.TNTUltimateTrigger TNT_ULTIMATE = CriteriaTriggers.register(new com.example.zitraksmode.criteria.TNTUltimateTrigger());
    public static final TNTExplosionKillTrigger TNT_EXPLOSION_KILL = CriteriaTriggers.register(new TNTExplosionKillTrigger());
    public static final SniperLongShotTrigger SNIPER_LONG_SHOT = CriteriaTriggers.register(new SniperLongShotTrigger());
    public static final com.example.zitraksmode.criteria.BombardierSetTrigger BOMBARDIER_SET = CriteriaTriggers.register(new com.example.zitraksmode.criteria.BombardierSetTrigger());
    public static final MinerDrainedTrigger MINER_DRAINED = CriteriaTriggers.register(new MinerDrainedTrigger());
    public static final KillAdunTrigger KILL_ADUN = CriteriaTriggers.register(new KillAdunTrigger());
    public static final LabubuCollectorTrigger LABUBU_COLLECTOR = CriteriaTriggers.register(new LabubuCollectorTrigger());
    public static final EnderIntruderTrigger ENDER_INTRUDER_CAUGHT = CriteriaTriggers.register(new EnderIntruderTrigger());
    public static final IntruderFuckedTrigger INTRUDER_FUCKED = CriteriaTriggers.register(new IntruderFuckedTrigger());
    public static final MihActivateTrigger MIH_ACTIVATE = CriteriaTriggers.register(new MihActivateTrigger());
    public static final UniverseResetTrigger UNIVERSE_RESET = CriteriaTriggers.register(new UniverseResetTrigger());
    public static final DildoSatLongTrigger DILDO_SAT_LONG = CriteriaTriggers.register(new DildoSatLongTrigger());
    public static final TNTDildoBrokenTrigger TNT_DILDO_BROKEN = CriteriaTriggers.register(new TNTDildoBrokenTrigger());
    public static final CreeperCastratedTrigger CREEPER_CASTRATED = CriteriaTriggers.register(new CreeperCastratedTrigger());
    public static final ChargedCreeperCastratedTrigger CHARGED_CREEPER_CASTRATED = CriteriaTriggers.register(new ChargedCreeperCastratedTrigger());
    public static final SuperTNTActivatedTrigger SUPER_TNT_ACTIVATED = CriteriaTriggers.register(new SuperTNTActivatedTrigger());
    public static final JBLFullVolumeTrigger JBL_FULL_VOLUME = CriteriaTriggers.register(new JBLFullVolumeTrigger());
    public static final KatanaUltimateTrigger KATANA_ULTIMATE = CriteriaTriggers.register(new KatanaUltimateTrigger());
    public static final KatanaParryTrigger KATANA_PARRY = CriteriaTriggers.register(new KatanaParryTrigger());
    public static final KatanaDeflectKillTrigger KATANA_DEFLECT_ARROW_KILL = CriteriaTriggers.register(new KatanaDeflectKillTrigger());
    public static final AqualungDeepBreathTrigger AQUALUNG_DEEP_BREATH = CriteriaTriggers.register(new AqualungDeepBreathTrigger());
    public static final AqualungDrownTrigger AQUALUNG_DROWN = CriteriaTriggers.register(new AqualungDrownTrigger());
    public static final PrinterHammerTrigger PRINTER_HAMMER = CriteriaTriggers.register(new PrinterHammerTrigger());
    public static final PrinterHouseTrigger PRINTER_HOUSE = CriteriaTriggers.register(new PrinterHouseTrigger());
    public static final PrinterPrisonTrigger PRINTER_PRISON = CriteriaTriggers.register(new PrinterPrisonTrigger());
    public static final HentaiMemeFacesTrigger HENTAI_MEME_FACES = CriteriaTriggers.register(new HentaiMemeFacesTrigger());
    public static final HentaiLongStareTrigger HENTAI_LONG_STARE = CriteriaTriggers.register(new HentaiLongStareTrigger());
    public static final PaintingPlacedTrigger PAINTING_BOOBS_PLACED = CriteriaTriggers.register(new PaintingPlacedTrigger());
    public static final KoranFullyReadTrigger KORAN_FULLY_READ = CriteriaTriggers.register(new KoranFullyReadTrigger());
    public static final SteelBallGoldenRotationTrigger STEEL_BALL_GOLDEN_ROTATION = CriteriaTriggers.register(new SteelBallGoldenRotationTrigger());
    public static final SteelBallKillTrigger STEEL_BALL_KILL_TRIGGER = CriteriaTriggers.register(new SteelBallKillTrigger());
    public static final DeathNoteAllMethodsTrigger DEATH_NOTE_ALL_METHODS = CriteriaTriggers.register(new DeathNoteAllMethodsTrigger());
    public static final GazanTradeTrigger GAZAN_TRADE = CriteriaTriggers.register(new GazanTradeTrigger());
    public static final FindMahoragaOutpostTrigger FIND_MAHORAGA_OUTPOST = CriteriaTriggers.register(new FindMahoragaOutpostTrigger());
    public static final MahoragaSummonedTrigger MAHORAGA_SUMMONED = CriteriaTriggers.register(new MahoragaSummonedTrigger());
    public static final MahoragaKilledTrigger MAHORAGA_KILLED = CriteriaTriggers.register(new MahoragaKilledTrigger());
    public static final MilkBullTrigger MILK_BULL = CriteriaTriggers.register(new MilkBullTrigger());
    public static final MilkRavagerTrigger MILK_RAVAGER = CriteriaTriggers.register(new MilkRavagerTrigger());
    public static final com.example.zitraksmode.criteria.MaxMessagesTrigger MAX_MESSAGES = CriteriaTriggers.register(new com.example.zitraksmode.criteria.MaxMessagesTrigger());
    public static final com.example.zitraksmode.criteria.MaxTradesTrigger MAX_TRADES = CriteriaTriggers.register(new com.example.zitraksmode.criteria.MaxTradesTrigger());
    public static final com.example.zitraksmode.criteria.MaxRejectsTrigger MAX_REJECTS = CriteriaTriggers.register(new com.example.zitraksmode.criteria.MaxRejectsTrigger());

    public ZitraksMode() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        LogiaNetwork.register();
        VacuumMenus.register(bus);
        VacuumNetwork.register();
        software.bernie.geckolib3.GeckoLib.initialize();
        ModMenuTypes.register(bus);
        ModSounds.register(bus);
        ModBlocks.register(bus);
        ModItems.register(bus);
        ModEntities.register(bus);
        ModBlockEntities.register(bus);
        ModEnchantments.register(bus);
        ModRecipeSerializers.SERIALIZERS.register(bus);
        ModVillagers.register(bus);
        ModPaintings.register(bus);
        ModRecipes.SERIALIZERS.register(bus);
        JBLSpeakerNetwork.register();
        ChigurNetwork.register();
        ModMessages.register();
        ModPackets.register();
        ScooterPackets.register();
        MedusaNetwork.register();
        MaxPackets.register();
        PacketHandler.registerPackets();
        com.example.zitraksmode.network.labubu.LabubuCasePackets.register();
        com.example.zitraksmode.network.mih.MIHMusicPackets.register();
        bus.addListener(ModSpawnEvents::setupSpawns);
        bus.addListener(this::commonSetup);
        bus.addListener(LogiaGaugeCapability::register);
        bus.addListener(this::clientSetup);
        bus.addListener(this::registerEntityAttributes);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.register(new CowMilkingHandler());
        MinecraftForge.EVENT_BUS.register(CowMilkingHandler.class);
        MinecraftForge.EVENT_BUS.register(new SecretAchievementsHandler());
        MinecraftForge.EVENT_BUS.register(AdunDeathHandler.class);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            NetworkHandler.register();
            SpawnPlacements.register(
                    ModEntities.ADUN_VILLAGER.get(),
                    SpawnPlacements.Type.ON_GROUND,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    AdunVillager::canSpawn
            );

            // === Регистрация поведения раздатчика для Super TNT ===
            // Заставляет раздатчик спавнить поджжённый Super TNT entity напрямую
            // (как ванильный TNT), а не выкидывать блок-предмет.
            SuperTNTDispenseSetup.register();
        });
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(RandomArtPools::init);
    }

    private void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.ADUN_VILLAGER.get(), AdunVillager.createAttributes().build());
        event.put(ModEntities.GAZAN.get(), GazanEntity.createAttributes().build());
        event.put(ModEntities.MAHORAGA.get(), MahoragaEntity.createAttributes().build());
        event.put(ModEntities.MAHORAGA_CINEMATIC.get(), MahoragaCinematicEntity.createAttributes().build());
        event.put(ModEntities.ANTON_CHIGUR.get(), AntonChigurEntity.createAttributes().build());
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ScanBuildCommand.register(event.getDispatcher());
        IntimacyCommand.register(event.getDispatcher());
        com.example.zitraksmode.commands.KatanaResetCooldownCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(EnderCapability.BIG_ENDER_INVENTORY).ifPresent(oldInv -> {
            event.getEntity().getCapability(EnderCapability.BIG_ENDER_INVENTORY).ifPresent(newInv -> {
                for (int i = 0; i < oldInv.getSlots(); i++) {
                    newInv.setStackInSlot(i, oldInv.getStackInSlot(i).copy());
                }
            });
        });
        PrinterRecipeCapability.copy(event.getOriginal(), event.getEntity());
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(
                    new ResourceLocation(MODID, "big_ender_inventory"),
                    new EnderCapability.Provider()
            );
            event.addCapability(
                    PrinterRecipeCapability.ID,
                    new PrinterRecipeCapability.Provider()
            );
        }
    }

    @SubscribeEvent
    public void onRegisterCaps(RegisterCapabilitiesEvent e) {
        LogiaGaugeCapability.register(e);
    }

   @SubscribeEvent
    public void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            MaxMessengerSavedData.get(serverPlayer.getServer()).ensureProfile(serverPlayer);
            MaxMessengerItem.giveIfMissing(serverPlayer, ModItems.MAX_MESSENGER.get());
        }
        syncPrinterRecipes(event.getEntity());
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        syncPrinterRecipes(event.getEntity());
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
        syncPrinterRecipes(event.getEntity());
    }

    private void syncPrinterRecipes(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        PrinterRecipeCapability.get(serverPlayer).ifPresent(library ->
                ModPackets.sendToPlayer(SyncPrinterRecipeLibraryPacket.fromLibrary(library), serverPlayer)
        );
    }
}
