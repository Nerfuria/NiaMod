package org.nia.niamod.features;

import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.chat.type.RecipientType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.eventbus.NiaEventBus;
import org.nia.niamod.eventbus.Subscribe;
import org.nia.niamod.mixin.ChatComponentAccessor;
import org.nia.niamod.models.config.ShoutReplacement;
import org.nia.niamod.models.events.ChatModifyEvent;
import org.nia.niamod.models.misc.ExecuteRunnableClickEvent;
import org.nia.niamod.models.misc.Feature;
import org.nia.niamod.models.misc.Safe;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("unused")
public class ShoutFilterFeature extends Feature {
    private static final TextColor GRAY_OUT_COLOR = TextColor.fromRgb(0x676767);

    @Override
    @Safe
    public void init() {
        NiaEventBus.subscribe(this);
    }

    @Subscribe
    public void modifyChat(ChatModifyEvent event) {
        Component component = event.getMessage();
        if (!NyahConfig.getData().isShoutFilterFeatureEnabled()) return;
        if (!RecipientType.SHOUT.matchPattern(StyledText.fromComponent(component))) return;

        if (NyahConfig.getData().getShoutFilterMode() == ShoutReplacement.REMOVE) {
            event.setMessage(null);
        } else if (NyahConfig.getData().getShoutFilterMode() == ShoutReplacement.GRAY_OUT) {
            event.setMessage(withColor(component));
        } else {
            Minecraft mc = Minecraft.getInstance();
            final int insertTick = mc.gui.getGuiTicks();

             MutableComponent toReplace = Component.literal("Shout hidden, click to open");

             toReplace.withStyle(Style.EMPTY
                     .withColor(TextColor.fromRgb(0x3b1344))
                     .withClickEvent(new ExecuteRunnableClickEvent(() -> {
                         List<GuiMessage> allMessages = ((ChatComponentAccessor) mc.gui.getChat()).niamod$allMessages();
                         for (int i = 0; i < allMessages.size(); i++) {
                             if (allMessages.get(i).content().equals(toReplace)) {
                                 allMessages.set(i, new GuiMessage(insertTick, component, null, GuiMessageTag.system()));
                                 ((ChatComponentAccessor) mc.gui.getChat()).niamod$refreshTrimmedMessages();
                                 return;
                             }
                         }
                     })));

            event.setMessage(toReplace);
        }
    }


    public static MutableComponent withColor(Component component) {
        MutableComponent copy = Component.empty();
        component.visit((style, string) -> {
            copy.append(Component.literal(legacyCode(string)).withStyle(style.withColor(GRAY_OUT_COLOR)));
            return Optional.empty();
        }, Style.EMPTY);
        return copy;
    }

    private static String legacyCode(String string) {
        StringBuilder builder = null;
        int lastCopied = 0;

        for (int i = 0; i < string.length(); i++) {
            if (string.charAt(i) != ChatFormatting.PREFIX_CODE || i + 1 >= string.length()) continue;

            int hexSequenceLength = leglen(string, i);
            if (hexSequenceLength > 0) {
                if (builder == null) builder = new StringBuilder(string.length());
                builder.append(string, lastCopied, i).append(ChatFormatting.RESET);
                i += hexSequenceLength - 1;
                lastCopied = i + 1;
                continue;
            }

            ChatFormatting formatting = ChatFormatting.getByCode(string.charAt(i + 1));
            if (formatting != null && formatting.isColor()) {
                if (builder == null) builder = new StringBuilder(string.length());
                builder.append(string, lastCopied, i).append(ChatFormatting.RESET);
                i++;
                lastCopied = i + 1;
            }
        }

        return builder == null ? string : builder.append(string, lastCopied, string.length()).toString();
    }

    private static int leglen(String string, int start) {
        if (start + 13 >= string.length() || Character.toLowerCase(string.charAt(start + 1)) != 'x') return 0;

        for (int i = start + 2; i <= start + 12; i += 2) {
            if (string.charAt(i) != ChatFormatting.PREFIX_CODE || Character.digit(string.charAt(i + 1), 16) == -1) {
                return 0;
            }
        }

        return 14;
    }

}
