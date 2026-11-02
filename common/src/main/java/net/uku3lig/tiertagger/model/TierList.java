package net.uku3lig.tiertagger.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Optional;

@Getter
@AllArgsConstructor
public enum TierList {
    MCTIERS("MCTiers", "https://mctiers.net/api", '\uE901'),
    SUBTIERS("SubTiers", "https://subtiers.net/api", '\uE902'),
    ;

    private final String name;
    private final String url;
    private final char icon;

    public Component styledName(boolean current) {
        if (current) {
            return Component.translatable("tiertagger.config.selectedTL", icon, name);
        } else {
            return Component.literal(icon + " " + name);
        }
    }

    public static Optional<TierList> findByUrl(String url) {
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);

        final String finalUrl = url; // i :heart: java
        return Arrays.stream(values()).filter(list -> list.url.equals(finalUrl)).findFirst();
    }
}
