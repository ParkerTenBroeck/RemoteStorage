package com.parkertenbroeck.remotestorage;

import com.parkertenbroeck.remotestorage.system.MemberSettings;
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
        this.uiAdapter = null;
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {

//        rootComponent
//                .surface(Surface.VANILLA_TRANSLUCENT)
//                .horizontalAlignment(HorizontalAlignment.CENTER)
//                .verticalAlignment(VerticalAlignment.CENTER);
//
//        var innerScroll = Containers.verticalFlow(Sizing.content(), Sizing.content());
//
//        rootComponent.child(
//                Containers.verticalFlow(Sizing.content(), Sizing.content())
//            .child(
//                Containers.verticalScroll(Sizing.content(), Sizing.fill(50),
//                        innerScroll
//                                .child(Components.button(Text.of("+ new +"), b -> add()).margins(Insets.of(5)))
//                                .children(RemoteStorageClient.system.groups().stream().map(g -> group(innerScroll, g)).toList())
//                                .horizontalAlignment(HorizontalAlignment.CENTER)
//                )
//                .surface(Surface.DARK_PANEL)
//                .padding(Insets.of(5))
//                .verticalAlignment(VerticalAlignment.CENTER)
//                .horizontalAlignment(HorizontalAlignment.CENTER)
//            )
//        );
    }

    private void add() {
//        this.client.setScreen(new EditGroupScreen(RemoteStorageClient.system.newGroup(), GroupListScreen::new));
    }


    private Component group(FlowLayout innerScroll, MemberSettings group){
        var c = Containers.horizontalFlow(Sizing.content(), Sizing.content());
        return c;
//                .child(
//                        Components.button(
//                                Text.of("#"+group.id + ": " + group.name()),
//                                b -> this.client.setScreen(new EditGroupScreen(RemoteStorageClient.system.settings(group.id), GroupListScreen::new))
//                        )
//                )
//                .child(Components.button(Text.of("D"), b -> {
//                    RemoteStorageClient.system.removeGroup(group.id);
//                    innerScroll.removeChild(c);
//                    innerScroll.onChildMutated(b);
//                }))
//                .padding(Insets.horizontal(5))
//                .padding(Insets.vertical(1))
//                .horizontalAlignment(HorizontalAlignment.LEFT)
//                .verticalAlignment(VerticalAlignment.CENTER);
    }
}
