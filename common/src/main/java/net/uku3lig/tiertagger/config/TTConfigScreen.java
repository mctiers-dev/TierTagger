package net.uku3lig.tiertagger.config;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.layouts.GridLayout;
import net.uku3lig.tiertagger.TierCache;
import net.uku3lig.tiertagger.TierTagger;
import net.uku3lig.tiertagger.model.TierList;
import net.uku3lig.tiertagger.tierlist.PlayerSearchScreen;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.uku3lig.ukulib.config.option.*;
import net.uku3lig.ukulib.config.option.widget.ButtonTab;
import net.uku3lig.ukulib.config.option.widget.TextInputWidget;
import net.uku3lig.ukulib.config.screen.TabbedConfigScreen;
import net.uku3lig.ukulib.utils.Ukutils;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

public class TTConfigScreen extends TabbedConfigScreen<TierTaggerConfig> {
    private final String onOpenApiUrl;

    public TTConfigScreen(Screen parent) {
        super("TierTagger Config", parent, TierTagger.getManager());
        this.onOpenApiUrl = TierTagger.getManager().getConfig().getApiUrl();
    }

    @Override
    public void onClose() {
        TierTaggerConfig config = this.manager.getConfig();
        if (!config.getApiUrl().equalsIgnoreCase(this.onOpenApiUrl)) {
            this.manager.saveConfig();
            TierCache.init();
            Component tlName = TierList.findByUrl(config.getApiUrl()).map(TierList::getName).map(Component::literal)
                    .orElse(Component.translatable("tiertagger.config.custom"));
            Ukutils.sendToast(Component.translatable("tiertagger.config.changed", tlName), Component.translatable("tiertagger.config.reloading"));
        }

        super.onClose();
    }

    @Override
    protected Tab[] getTabs(TierTaggerConfig config) {
        return new Tab[]{new MainSettingsTab(), new ColorsTab(), new TierlistTab()};
    }

    public class MainSettingsTab extends ButtonTab<TierTaggerConfig> {
        public MainSettingsTab() {
            super("tiertagger.config", TTConfigScreen.this.manager);
        }

        @Override
        protected WidgetCreator[] getWidgets(TierTaggerConfig config) {
            return new WidgetCreator[]{
                    CyclingOption.ofBoolean("tiertagger.config.enabled", config.isEnabled(), config::setEnabled),
                    new CyclingOption<>("tiertagger.config.gamemode", TierCache.getGamemodes(), config.getGameMode(), m -> config.setGameMode(m.id()), m -> Component.literal(m.title()),
                            m -> m.isNone() ? Tooltip.create(Component.translatable("tiertagger.config.gamemode.none")) : null, !config.getGameMode().isNone()),
                    CyclingOption.ofBoolean("tiertagger.config.retired", config.isShowRetired(), config::setShowRetired),
                    CyclingOption.ofTranslatableEnum("tiertagger.config.highest", TierTaggerConfig.HighestMode.class, config.getHighestMode(), config::setHighestMode, OptionInstance.cachedConstantTooltip(Component.translatable("tiertagger.config.highest.desc"))),
                    CyclingOption.ofBoolean("tiertagger.config.icons", config.isShowIcons(), config::setShowIcons),
                    CyclingOption.ofBoolean("tiertagger.config.playerList", config.isPlayerList(), config::setPlayerList),
                    new SimpleButton("tiertagger.clear", _ -> TierCache.clearCache()),
                    new ScreenOpenButton("tiertagger.config.search", PlayerSearchScreen::new)
            };
        }
    }

    public class TierlistTab extends GridLayoutTab {
        public TierlistTab() {
            super(Component.translatable("tiertagger.config.tierlists"));
            TierTaggerConfig config = TTConfigScreen.this.manager.getConfig();
            Optional<TierList> current = TierList.findByUrl(config.getApiUrl());

            GridLayout.RowHelper helper = this.layout.rowSpacing(4).createRowHelper(1);

            List<AbstractWidget> buttons = Arrays.stream(TierList.values())
                    .map(t -> {
                        boolean isCurrent = current.isPresent() && current.get() == t;
                        return new SimpleButton(t.styledName(isCurrent), _ -> {
                            config.setApiUrl(t.getUrl());
                            TTConfigScreen.this.onClose();
                        }, !isCurrent);
                    })
                    .map(w -> w.createWidget(0, 0, 210, 20))
                    .map(helper::addChild)
                    .toList();

            TextInputWidget customInput = new TextInputWidget(0, 0, 210, 20, config.getApiUrl(), config::setApiUrl,
                    Component.translatable("tiertagger.config.apiUrl").getString(),
                    this::isValidURI, 1000);
            customInput.visible = current.isEmpty();

            AbstractWidget customButton = new SimpleButton(Component.translatable("tiertagger.config.custom"), self -> {
                buttons.forEach(w -> w.active = true);
                customInput.visible = true;
                self.active = false;
                customInput.setValue("");
            }, current.isPresent()).createWidget(0, 0, 210, 20);

            helper.addChild(customButton);
            helper.addChild(customInput);
        }

        private boolean isValidURI(String uri) {
            try {
                return new URI(uri).getScheme().startsWith("http");
            } catch (Exception e) {
                return false;
            }
        }
    }

    public class ColorsTab extends ButtonTab<TierTaggerConfig> {
        protected ColorsTab() {
            super("tiertagger.colors", TTConfigScreen.this.manager);
        }

        @Override
        protected WidgetCreator[] getWidgets(TierTaggerConfig config) {
            // i genuinely don't understand but chaining the calls just EXPLODES????
            Comparator<Map.Entry<String, Integer>> comparator = Comparator.comparing(e -> e.getKey().charAt(2));
            comparator = comparator.thenComparing(e -> e.getKey().charAt(0));

            List<ColorOption> tiers = config.getTierColors().entrySet().stream()
                    .sorted(comparator)
                    .map(e -> new ColorOption(e.getKey(), e.getValue(), val -> config.getTierColors().put(e.getKey(), val)))
                    .collect(Collectors.toList());

            tiers.addLast(new ColorOption("tiertagger.colors.retired", config.getRetiredColor(), config::setRetiredColor));

            return tiers.toArray(WidgetCreator[]::new);
        }
    }
}
