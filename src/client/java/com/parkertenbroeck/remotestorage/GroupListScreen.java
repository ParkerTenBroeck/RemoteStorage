package com.parkertenbroeck.remotestorage;

import com.parkertenbroeck.remotestorage.system.Group;
import io.wispforest.owo.ui.base.BaseOwoScreen;
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

        var meow = Containers.verticalFlow(Sizing.content(), Sizing.content());
                Containers.verticalScroll(Sizing.content(), Sizing.fill(50),
                    Containers.verticalFlow(Sizing.content(), Sizing.content())
                            .children(RemoteStorageClient.system.groups().stream().map(this::group).toList())
                )
                .surface(Surface.DARK_PANEL)
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.CENTER);

        rootComponent.child(meow);
    }



    private Component group(Group group){
        return Containers.verticalFlow(Sizing.content(), Sizing.content())
                .child(Components.label(Text.of(group.id + ": " + group.name())))
                .child(Components.label(Text.of("Input: " + group.input().priority() + "Output: " + group.output().priority())))
                .surface(Surface.PANEL)
                .padding(Insets.of(5))
                .horizontalAlignment(HorizontalAlignment.LEFT)
                .verticalAlignment(VerticalAlignment.CENTER);
    }
}
