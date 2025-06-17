package com.parkertenbroeck.remotestorage;

import com.parkertenbroeck.remotestorage.system.Group;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

@Environment(EnvType.CLIENT)
public class GroupListScreen extends BaseOwoScreen<FlowLayout> {

    ArrayList<Component> list = new ArrayList<>();

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent
                .surface(Surface.VANILLA_TRANSLUCENT)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);

        rootComponent.child(
                Containers.verticalFlow(Sizing.content(), Sizing.content())
            .child(
                Containers.verticalScroll(Sizing.content(), Sizing.fill(50),
                        Containers.verticalFlow(Sizing.content(), Sizing.content())
                                .child(Components.button(Text.of("+ new +"), this::add).margins(Insets.of(5)))
                                .children(RemoteStorageClient.system.groups().stream().map(this::group).toList())
                                .horizontalAlignment(HorizontalAlignment.CENTER)
                )
                .surface(Surface.DARK_PANEL)
                .padding(Insets.of(5))
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.CENTER)
            )
        );
    }

    private void add(ButtonComponent buttonComponent) {
        this.client.setScreen(new EditGroupScreen(RemoteStorageClient.system.newGroup()));
    }


    private Component group(Group group){
        return Containers.horizontalFlow(Sizing.content(), Sizing.content())
                .child(
                        Components.button(
                                Text.of("#"+group.id + ": " + group.name()),
                                b -> {}
                        )
                )
                .child(Components.button(Text.of("D"), b -> {}))
                .padding(Insets.of(5))
                .horizontalAlignment(HorizontalAlignment.LEFT)
                .verticalAlignment(VerticalAlignment.CENTER);
    }
}
