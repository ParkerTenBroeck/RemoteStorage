package com.parkertenbroeck.remotestorage;

import com.parkertenbroeck.remotestorage.system.Configuration;
import com.parkertenbroeck.remotestorage.system.Group;
import com.parkertenbroeck.remotestorage.system.ListKind;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

public class EditGroupScreen extends BaseOwoScreen<FlowLayout> {

    private final Group group;

    public EditGroupScreen(Group group) {
        this.group = group;
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter () {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent){

        rootComponent
                .surface(Surface.VANILLA_TRANSLUCENT)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);

        var nameTextBox = Components.textBox(Sizing.fill(40), group.name());
        nameTextBox.onChanged().subscribe(group::setName);

        var inputContainer = Containers.collapsible(Sizing.content(), Sizing.content(), Text.of("input"), true);
        var outputContainer = Containers.collapsible(Sizing.content(), Sizing.content(), Text.of("output"), false);

        inputContainer.child(configurationComponent(group.input()));
        inputContainer.titleLayout().horizontalAlignment(HorizontalAlignment.LEFT);
        inputContainer.onToggled().subscribe(expanded -> {
            if(outputContainer.expanded()&&expanded)
                outputContainer.toggleExpansion();
        });

        outputContainer.child(configurationComponent(group.output()));
        outputContainer.titleLayout().horizontalAlignment(HorizontalAlignment.LEFT);
        outputContainer.onToggled().subscribe(expanded -> {
            if(inputContainer.expanded()&&expanded)
                inputContainer.toggleExpansion();
        });


        rootComponent.child(
            Containers.verticalFlow(Sizing.content(), Sizing.content())
                .child(Components.label(Text.of("Group ID: " + group.id)))
                .child(nameTextBox)
                .child(
                        Containers.verticalFlow(Sizing.content(), Sizing.content())
                            .child(inputContainer)
                            .child(outputContainer)
                            .horizontalAlignment(HorizontalAlignment.LEFT)
                )
                .child(
                        Containers.horizontalFlow(Sizing.content(), Sizing.content())
                                .child(Components.button(Text.of("Save"), this::save))
                                .child(Components.button(Text.of("Cancel"), this::cancel))
                )
                .padding(Insets.of(5))
                .surface(Surface.DARK_PANEL)
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.CENTER)
        );

    }

    @Override
    public void close() {
        save(null);
        super.close();
    }

    private void cancel(ButtonComponent buttonComponent) {
    }

    private void save(ButtonComponent buttonComponent) {
        RemoteStorageClient.system.updateGroup(group);
    }

    private Component configurationComponent(Configuration configuration){
        var blacklist = Components.checkbox(Text.of("Blacklist"))
                            .checked(configuration.kind() == ListKind.Blacklist);
        var whitelist = Components.checkbox(Text.of("Whitelist"))
                            .checked(configuration.kind() == ListKind.Whitelist);

        blacklist.onChanged(checked -> {
            whitelist.checked(!checked);
            configuration.setKind(checked? ListKind.Blacklist: ListKind.Whitelist);
        });
        whitelist.onChanged(checked -> {
            blacklist.checked(!checked);
            configuration.setKind(checked? ListKind.Whitelist: ListKind.Blacklist);
        });

        var prioritySlider = Components.discreteSlider(Sizing.fill(40), -100, 100).setFromDiscreteValue(configuration.priority()).message(i -> Text.of("Priority " + i));
        prioritySlider.scrollStep(1.0/200.0);
        prioritySlider.onChanged().subscribe(value -> configuration.setPriority((int) value));

        return Containers.verticalFlow(Sizing.content(), Sizing.content())
                .child(prioritySlider).gap(2)
                .child(Containers.horizontalFlow(Sizing.content(), Sizing.content()).child(blacklist).child(whitelist))
                .gap(2)
                .child(Containers.verticalScroll(Sizing.fill(40), Sizing.fill(20),
                        Containers.verticalFlow(Sizing.fill(), Sizing.content())
                                .child(Components.button(Text.of("+"), b -> {}))
                                .surface(Surface.DARK_PANEL)
                ))
                .padding(Insets.of(5));
    }
}