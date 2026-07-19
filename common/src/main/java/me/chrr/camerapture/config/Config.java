package me.chrr.camerapture.config;


import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.chrr.camerapture.domain.config.CurrencyConfig;
import me.chrr.camerapture.domain.config.EntityValueConfig;
import me.chrr.camerapture.domain.config.GameplayConfig;
import me.chrr.camerapture.domain.config.ScoringConfig;
import me.chrr.camerapture.domain.config.RegistryScanConfig;
import me.chrr.camerapture.domain.config.BiomeObservationConfig;
import me.chrr.camerapture.domain.config.ValuationDebugConfig;
import net.minecraft.util.Identifier;

public class Config {
    public static Config DEFAULT = new Config();

    public Client client = new Client();
    public Server server = new Server();

    /// Client-specific config options.
    public static class Client {
        public int version = 4;

        public boolean cachePictures = false;
        public boolean saveScreenshot = false;
        public boolean simpleCameraHud = false;
        public float minimumZoomSensitivity = ZoomSensitivityCurve.DEFAULT_MINIMUM;
        public float zoomSensitivityExponent = ZoomSensitivityCurve.DEFAULT_EXPONENT;

        @DeprecatedConfigOption
        private float zoomMouseSensitivity = 0.5f;

        public void upgrade() {
            if (this.version < 4) {
                this.minimumZoomSensitivity = ZoomSensitivityCurve.sanitizeMinimum(this.zoomMouseSensitivity);
            }
            this.minimumZoomSensitivity = ZoomSensitivityCurve.sanitizeMinimum(this.minimumZoomSensitivity);
            this.zoomSensitivityExponent = ZoomSensitivityCurve.sanitizeExponent(this.zoomSensitivityExponent);
            this.version = DEFAULT.client.version;
        }
    }

    /// Server-specific config options.
    public static class Server {
        public int version = 7;

        public int maxImageBytes = 500_000;
        public int maxImageResolution = 1920;
        public int msPerPicture = 20;
        public boolean canRotatePictures = true;
        public boolean checkFramePosition = false;

        public PermissionLevels permissionLevels = new PermissionLevels();
        public Expedition expedition = new Expedition();

        @DeprecatedConfigOption
        private boolean allowUploading = true;

        public void upgrade() {
            if (this.version < 5) {
                this.permissionLevels.upload = this.allowUploading ? 0 : 4;
            }
            if (this.expedition == null) {
                this.expedition = new Expedition();
            }
            if (this.expedition.currency == null) this.expedition.currency = new Expedition.Currency();
            if (this.expedition.entityValues == null) this.expedition.entityValues = new Expedition.EntityValues();
            if (this.expedition.rewards == null) this.expedition.rewards = new Expedition.Rewards();
            if (this.expedition.registryScan == null) this.expedition.registryScan = new Expedition.RegistryScan();
            if (this.expedition.biomeObservation == null) this.expedition.biomeObservation = new Expedition.BiomeObservation();
            if (this.expedition.valuation == null) this.expedition.valuation = new Expedition.Valuation();

            this.version = DEFAULT.server.version;
        }

        /** Validated immutable view consumed by server-authoritative gameplay services. */
        public GameplayConfig gameplayConfig() {
            return new GameplayConfig(
                    GameplayConfig.CURRENT_SCHEMA_VERSION,
                    new CurrencyConfig(Identifier.of(expedition.currency.provider), expedition.currency.teamShared),
                    new EntityValueConfig(
                            expedition.entityValues.healthWeight,
                            expedition.entityValues.armorWeight,
                            expedition.entityValues.attackWeight,
                            expedition.entityValues.specialWeight,
                            expedition.entityValues.minimumValue,
                            expedition.entityValues.maximumValue
                    ),
                    new ScoringConfig(
                            expedition.rewards.algorithmVersion,
                            expedition.rewards.secondaryWeight,
                            expedition.rewards.tertiaryWeight,
                            expedition.rewards.entityDiscoveryMultiplier,
                            expedition.rewards.biomeDiscoveryMultiplier,
                            expedition.rewards.typeDecayCoefficient,
                            expedition.rewards.typeDecayExponent,
                            expedition.rewards.maxPaidPerEntityInstance,
                            expedition.rewards.maxPaidPerEntityType,
                            expedition.rewards.maxPaidPerBiomeType,
                            expedition.rewards.minimumReward,
                            expedition.rewards.maximumReward
                    ),
                    new RegistryScanConfig(
                            expedition.registryScan.enabled,
                            expedition.registryScan.rebuildOnFingerprintChange,
                            expedition.registryScan.includeNonLivingEntities,
                            expedition.registryScan.globalEntityDefault,
                            expedition.registryScan.globalBiomeDefault,
                            expedition.registryScan.hostileMultiplier
                    ),
                    new BiomeObservationConfig(
                            expedition.biomeObservation.enabled,
                            expedition.biomeObservation.intervalTicks,
                            expedition.biomeObservation.minimumSamples,
                            expedition.biomeObservation.smoothingAlpha,
                            expedition.biomeObservation.minimumMultiplier,
                            expedition.biomeObservation.maximumMultiplier,
                            expedition.biomeObservation.filterBits
                    ),
                    new ValuationDebugConfig(
                            expedition.valuation.logUnknownEntities,
                            expedition.valuation.logEmptyTags,
                            expedition.valuation.debugCommands
                    )
            );
        }

        public static class Expedition {
            public Currency currency = new Currency();
            public EntityValues entityValues = new EntityValues();
            public Rewards rewards = new Rewards();
            public RegistryScan registryScan = new RegistryScan();
            public BiomeObservation biomeObservation = new BiomeObservation();
            public Valuation valuation = new Valuation();

            public static class Currency {
                public String provider = CurrencyConfig.INTERNAL_PROVIDER.toString();
                public boolean teamShared = false;
            }

            public static class EntityValues {
                public double healthWeight = EntityValueConfig.DEFAULT.healthWeight();
                public double armorWeight = EntityValueConfig.DEFAULT.armorWeight();
                public double attackWeight = EntityValueConfig.DEFAULT.attackWeight();
                public double specialWeight = EntityValueConfig.DEFAULT.specialWeight();
                public long minimumValue = EntityValueConfig.DEFAULT.minimumValue();
                public long maximumValue = EntityValueConfig.DEFAULT.maximumValue();
            }

            public static class Rewards {
                public int algorithmVersion = ScoringConfig.DEFAULT.algorithmVersion();
                public double secondaryWeight = ScoringConfig.DEFAULT.secondaryWeight();
                public double tertiaryWeight = ScoringConfig.DEFAULT.tertiaryWeight();
                public double entityDiscoveryMultiplier = ScoringConfig.DEFAULT.entityDiscoveryMultiplier();
                public double biomeDiscoveryMultiplier = ScoringConfig.DEFAULT.biomeDiscoveryMultiplier();
                public double typeDecayCoefficient = ScoringConfig.DEFAULT.typeDecayCoefficient();
                public double typeDecayExponent = ScoringConfig.DEFAULT.typeDecayExponent();
                public int maxPaidPerEntityInstance = ScoringConfig.DEFAULT.maxPaidPerEntityInstance();
                public int maxPaidPerEntityType = ScoringConfig.DEFAULT.maxPaidPerEntityType();
                public int maxPaidPerBiomeType = ScoringConfig.DEFAULT.maxPaidPerBiomeType();
                public long minimumReward = ScoringConfig.DEFAULT.minimumReward();
                public long maximumReward = ScoringConfig.DEFAULT.maximumReward();
            }

            public static class RegistryScan {
                public boolean enabled = RegistryScanConfig.DEFAULT.enabled();
                public boolean rebuildOnFingerprintChange = RegistryScanConfig.DEFAULT.rebuildOnFingerprintChange();
                public boolean includeNonLivingEntities = RegistryScanConfig.DEFAULT.includeNonLivingEntities();
                public long globalEntityDefault = RegistryScanConfig.DEFAULT.globalEntityDefault();
                public long globalBiomeDefault = RegistryScanConfig.DEFAULT.globalBiomeDefault();
                public double hostileMultiplier = RegistryScanConfig.DEFAULT.hostileMultiplier();
            }

            public static class BiomeObservation {
                public boolean enabled = BiomeObservationConfig.DEFAULT.enabled();
                public int intervalTicks = BiomeObservationConfig.DEFAULT.intervalTicks();
                public long minimumSamples = BiomeObservationConfig.DEFAULT.minimumSamples();
                public double smoothingAlpha = BiomeObservationConfig.DEFAULT.smoothingAlpha();
                public double minimumMultiplier = BiomeObservationConfig.DEFAULT.minimumMultiplier();
                public double maximumMultiplier = BiomeObservationConfig.DEFAULT.maximumMultiplier();
                public int filterBits = BiomeObservationConfig.DEFAULT.filterBits();
            }

            public static class Valuation {
                public boolean logUnknownEntities = ValuationDebugConfig.DEFAULT.logUnknownEntities();
                public boolean logEmptyTags = ValuationDebugConfig.DEFAULT.logEmptyTags();
                public boolean debugCommands = ValuationDebugConfig.DEFAULT.debugCommands();
            }
        }

        /// Permission levels for various actions that can be taken by players.
        public static class PermissionLevels {
            public static Codec<PermissionLevels> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("takePicture").forGetter(p -> p.takePicture),
                    Codec.INT.fieldOf("upload").forGetter(p -> p.upload)
            ).apply(instance, PermissionLevels::new));

            public PermissionLevels() {
            }

            public PermissionLevels(int takePicture, int upload) {
                this.takePicture = takePicture;
                this.upload = upload;
            }

            public int takePicture = 0;
            public int upload = 0;

            @Override
            public String toString() {
                return "{takePicture=" + takePicture +
                        ", upload=" + upload +
                        '}';
            }
        }
    }
}
