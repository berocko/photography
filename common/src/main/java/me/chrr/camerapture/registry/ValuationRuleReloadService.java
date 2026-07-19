package me.chrr.camerapture.registry;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import me.chrr.camerapture.domain.valuation.LoadedValuationRules;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.BooleanSupplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Transactional server-data reload listener shared by both loaders. */
public final class ValuationRuleReloadService extends SinglePreparationResourceReloader<ValuationRuleReloadService.Prepared> {
    private static final String ROOT = "photo_values";
    private static final Logger LOGGER = LogManager.getLogger("Camerapture/ValuationRules");

    private final ValuationRuleResourceParser parser = new ValuationRuleResourceParser();
    private final Supplier<ValuationRuleResourceParser.ValidationContext> validationContext;
    private final Consumer<LoadedValuationRules> publisher;
    private final BooleanSupplier logUnknownTargets;
    private final BooleanSupplier logEmptyTags;
    private final AtomicReference<LoadedValuationRules> lastSuccessful = new AtomicReference<>(LoadedValuationRules.empty());

    public ValuationRuleReloadService(
            Supplier<ValuationRuleResourceParser.ValidationContext> validationContext,
            Consumer<LoadedValuationRules> publisher
    ) {
        this(validationContext, publisher, () -> true, () -> true);
    }

    public ValuationRuleReloadService(
            Supplier<ValuationRuleResourceParser.ValidationContext> validationContext,
            Consumer<LoadedValuationRules> publisher,
            BooleanSupplier logUnknownTargets,
            BooleanSupplier logEmptyTags
    ) {
        this.validationContext = Objects.requireNonNull(validationContext, "validationContext");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
        this.logUnknownTargets = Objects.requireNonNull(logUnknownTargets, "logUnknownTargets");
        this.logEmptyTags = Objects.requireNonNull(logEmptyTags, "logEmptyTags");
    }

    public LoadedValuationRules lastSuccessful() {
        return lastSuccessful.get();
    }

    public LoadedValuationRules loadNow(ResourceManager manager) {
        return readAndParse(manager);
    }

    public void publishNow(LoadedValuationRules rules) {
        apply(Prepared.success(rules), null, null);
    }

    @Override
    protected Prepared prepare(ResourceManager manager, Profiler profiler) {
        try {
            return Prepared.success(readAndParse(manager));
        } catch (RuntimeException exception) {
            return Prepared.failure(exception);
        }
    }

    private LoadedValuationRules readAndParse(ResourceManager manager) {
        Map<Identifier, JsonElement> resources = new HashMap<>();
        for (Map.Entry<Identifier, Resource> entry : manager.findResources(
                ROOT, id -> id.getPath().endsWith(".json")
        ).entrySet()) {
            try (BufferedReader reader = entry.getValue().getReader()) {
                resources.put(entry.getKey(), JsonParser.parseReader(reader));
            } catch (Exception exception) {
                resources.put(entry.getKey(), malformed(exception));
            }
        }
        return parser.parse(resources, validationContext.get(), System.currentTimeMillis());
    }

    @Override
    protected void apply(Prepared prepared, ResourceManager manager, Profiler profiler) {
        if (prepared.failure() != null) {
            LOGGER.error("valuation rule reload failed globally; keeping previous catalog", prepared.failure());
            return;
        }
        try {
            publisher.accept(prepared.rules());
            lastSuccessful.set(prepared.rules());
            prepared.rules().skipped().stream()
                    .filter(error -> logUnknownTargets.getAsBoolean() || !error.reason().contains("not present in the registry"))
                    .forEach(error -> LOGGER.error("skipping valuation rule {}: {}", error.resourceId(), error.reason()));
            ValuationRuleResourceParser.ValidationContext context = validationContext.get();
            boolean registryReady = !context.entityIds().isEmpty() || !context.biomeIds().isEmpty();
            if (registryReady && logEmptyTags.getAsBoolean()) {
                prepared.rules().notices().forEach(notice -> LOGGER.info("valuation rule: {}", notice));
            }
        } catch (RuntimeException exception) {
            LOGGER.error("valuation catalog rebuild failed; keeping previous catalog", exception);
        }
    }

    private static JsonElement malformed(Exception exception) {
        com.google.gson.JsonObject object = new com.google.gson.JsonObject();
        object.addProperty("__camerapture_read_error", exception.getMessage());
        return object;
    }

    public record Prepared(LoadedValuationRules rules, RuntimeException failure) {
        static Prepared success(LoadedValuationRules rules) {
            return new Prepared(rules, null);
        }

        static Prepared failure(RuntimeException failure) {
            return new Prepared(null, failure);
        }
    }
}
