package com.parkertenbroeck.remotestorage.system;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.parkertenbroeck.remotestorage.ItemData;
import com.parkertenbroeck.remotestorage.RemoteStorage;
import net.minecraft.component.Component;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.SimpleRegistry;
import net.minecraft.util.Identifier;

import java.util.Objects;

public interface Filter {
    Codec<Filter> CODEC = FilterType.REGISTRY.getCodec().dispatch(Filter::type, FilterType::codec);

    FilterType<?> type();
    boolean matches(ItemData item);

    class FilterTypes{
        public static final FilterType<TagMatch> TAG_MATCH = register("tag_match", new FilterType<>(TagMatch.CODEC));
        public static final FilterType<ItemMatch> ITEM_MATCH = register("item_match", new FilterType<>(ItemMatch.CODEC));
        public static final FilterType<ComponentMatch> COMPONENT_MATCH = register("component_match", new FilterType<>(ComponentMatch.CODEC));
        public static final FilterType<ItemDataMatch> ITEM_DATA_MATCH = register("item_data_match", new FilterType<>(ItemDataMatch.CODEC));
        public static final FilterType<ComponentTypeMatch> COMPONENT_TYPE_MATCH = register("component_type_match", new FilterType<>(ComponentTypeMatch.CODEC));

        public static <T extends Filter> FilterType<T> register(String id, FilterType<T> beanType) {
            return Registry.register(FilterType.REGISTRY, Identifier.of(RemoteStorage.MOD_ID, id), beanType);
        }
    }

    record FilterType<T extends Filter>(MapCodec<T> codec){
        public static final Registry<FilterType<?>> REGISTRY = new SimpleRegistry<>(RegistryKey.ofRegistry(Identifier.of(RemoteStorage.MOD_ID, "filter_types")), Lifecycle.stable());
    }

    record TagMatch(Identifier tag) implements Filter {
        public static final MapCodec<TagMatch> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Identifier.CODEC.fieldOf("tag").forGetter(TagMatch::tag)
                ).apply(instance, TagMatch::new)
        );

        @Override
        public FilterType<TagMatch> type() {
            return FilterTypes.TAG_MATCH;
        }

        @Override
        public boolean matches(ItemData item) {
            return item.item().streamTags().anyMatch(tag -> tag.id().equals(this.tag));
        }
    }

    record ItemMatch(Item item) implements Filter {
        public static final MapCodec<ItemMatch> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Item.ENTRY_CODEC.fieldOf("item").forGetter(i -> i.item.getRegistryEntry())
                ).apply(instance, i -> new ItemMatch(i.value()))
        );

        @Override
        public FilterType<ItemMatch> type() {
            return FilterTypes.ITEM_MATCH;
        }

        @Override
        public boolean matches(ItemData item) {
            return item.item().getItem() == this.item;
        }
    }

    record ComponentMatch(Component<?> component) implements Filter {
        public static final MapCodec<ComponentMatch> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        ComponentChanges.CODEC.fieldOf("component").forGetter(c -> ComponentChanges.builder().add(c.component).build())
                ).apply(instance, ComponentMatch::fromChanges)
        );

        private static ComponentMatch fromChanges(ComponentChanges changes) {
            var component = changes.entrySet().stream().map(e -> Component.of(e.getKey(), e.getValue().get())).findFirst().get();
            return new ComponentMatch(component);
        }

        @Override
        public FilterType<ComponentMatch> type() {
            return FilterTypes.COMPONENT_MATCH;
        }

        @Override
        public boolean matches(ItemData item) {
            return Objects.equals(item.item().getComponents().get(component.type()), component.value());
        }
    }

    record ComponentTypeMatch(ComponentType<?> componentType) implements Filter{
        public static final MapCodec<ComponentTypeMatch> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        ComponentType.CODEC.fieldOf("component_type").forGetter(c -> c.componentType)
                ).apply(instance, ComponentTypeMatch::new)
        );

        @Override
        public FilterType<ComponentTypeMatch> type() {
            return FilterTypes.COMPONENT_TYPE_MATCH;
        }

        @Override
        public boolean matches(ItemData item) {
            return item.item().getComponents().contains(componentType);
        }
    }

    record ItemDataMatch(ItemData exact) implements Filter {
        public static final MapCodec<ItemDataMatch> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        ItemData.CODEC.fieldOf("exact").forGetter(c -> c.exact)
                ).apply(instance, ItemDataMatch::new)
        );

        @Override
        public FilterType<ItemDataMatch> type() {
            return FilterTypes.ITEM_DATA_MATCH;
        }

        @Override
        public boolean matches(ItemData item) {
            return item.equals(exact);
        }
    }
}
