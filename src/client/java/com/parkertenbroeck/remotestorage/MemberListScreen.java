package com.parkertenbroeck.remotestorage;

import com.parkertenbroeck.remotestorage.system.Position;
import com.parkertenbroeck.remotestorage.system.StorageMember;
import com.parkertenbroeck.remotestorage.system.StorageSystem;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.CollapsibleContainer;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;

@Environment(EnvType.CLIENT)
public class MemberListScreen extends BaseOwoScreen<FlowLayout> {

    private final StorageSystem system;

    public MemberListScreen(StorageSystem system) {
        this.system = system;
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    private class Member{
        final StorageMember member;
        final ArrayList<Member> members = new ArrayList<>();

        public Member(StorageMember m) {
            member = m;
        }

        public Component build(){
            var c = Containers.verticalFlow(Sizing.content(), Sizing.content());

            if(member==null){
                c.child(
                        Containers.horizontalFlow(Sizing.content(), Sizing.content())
                            .child(
                                    Components.label(Text.of("<INVALID MEMBER>")).color(Color.RED).shadow(true)
                            ).padding(Insets.of(4))
                );
            }else{
                c.child(
                        Containers.horizontalFlow(Sizing.content(), Sizing.content())
                            .child(Components.button(Text.of(member.name()), b -> {
                                client.setScreen(new EditMemberScreen(system, member, () -> new MemberListScreen(system)));
                            }))
                            .child(Components.button(Text.of("D"), b -> {
                                system.remove(member.pos());
                                client.setScreen(new MemberListScreen(system));
                            })).padding(Insets.of(4))
                );
            }
            if(!members.isEmpty()){
                c.child(
                        Containers.collapsible(Sizing.content(), Sizing.content(), Text.of("Childern"), false)
                                .children(members.stream().map(Member::build).toList())
                );
            }

            return c.surface(Surface.DARK_PANEL);
        }
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent
                .surface(Surface.VANILLA_TRANSLUCENT)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);

        var topLevel = new ArrayList<Member>();
        var unProcessedMap = new HashMap<Position, StorageMember>();
        var processedMap = new HashMap<Position, Member>();

        system.unorderedMembers().forEach(m -> unProcessedMap.put(m.pos(), m));

        while(!unProcessedMap.isEmpty()){
            unProcessedMap.values().removeIf(m -> {
                if(m.linked().isEmpty()){
                    var mm = new Member(m);
                    topLevel.add(mm);
                    processedMap.put(m.pos(), mm);
                    return true;
                }else if(processedMap.get(m.linked().get()) instanceof Member mm){
                    processedMap.put(m.pos(), new Member(m));
                    mm.members.add(processedMap.get(m.pos()));
                    return true;
                }else if(!unProcessedMap.containsKey(m.linked().get())){
                    var mm = new Member(null);
                    topLevel.add(mm);
                    processedMap.put(m.linked().get(), mm);
                    mm.members.add(new Member(m));
                    return true;
                }

                return false;
            });
        }

        var meow = Containers.verticalFlow(Sizing.content(), Sizing.content())
                .child(
                        Containers.verticalScroll(Sizing.content(), Sizing.fill(75),
                            Containers.verticalFlow(Sizing.content() /**/, Sizing.content())
                                .children(topLevel.stream().map(Member::build).toList())
                                .padding(Insets.of(5))
                    ))
                .surface(Surface.DARK_PANEL)
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.CENTER);

        rootComponent.child(meow);
    }
}
