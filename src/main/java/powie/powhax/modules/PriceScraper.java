package powie.powhax.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import powie.powhax.Powhax;
import powie.powhax.utils.Config;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PriceScraper extends Module {
    private static final Pattern pricePattern = Pattern.compile("(Sell|Buy): (\\d+\\.\\d+)");
    private final Map<String, ItemForSale> itemsForSale = new TreeMap<>();

    public PriceScraper() {
        super(Powhax.CATEGORY, "price-scraper", "automatically scrapes the prices of items in dynamic shop");
    }

    @Override
    public void onDeactivate() {
        Path filePath = Config.writeNewShopData(itemsForSale);
        MutableComponent message = Component.literal("Saved shop data to: ");
        message.append(Component.literal(filePath.getFileName().toString())
            .withStyle(style -> style
                .applyFormat(ChatFormatting.YELLOW)
                .withClickEvent(new ClickEvent.OpenFile(filePath))
            )
        );
        info(message);
        itemsForSale.clear();
    }

    @EventHandler
    private void onTickPost(TickEvent.Post event) {
        if (!Utils.canUpdate()
            || !(mc.gui.screen() instanceof AbstractContainerScreen<?> screen)
            || screen.getMenu().getType() != MenuType.GENERIC_9x6)
            return;

        AbstractContainerMenu menu = screen.getMenu();

        // barrier is slot 45
        if (menu.getSlot(45).getItem().getItem() != Items.BARRIER) return;

        for (int i = 0; i < 45; i++) {
            ItemStack stack = menu.getSlot(i).getItem();
            if (stack.isEmpty()) continue;

            String itemName = stack.getItem().toString().substring(10);
            if (itemsForSale.containsKey(itemName)) continue; // skip if already scraped

            List<Component> tooltip = stack.getTooltipLines(
                Item.TooltipContext.of(mc.level),
                mc.player,
                TooltipFlag.Default.NORMAL
            );

            Float sellPrice = null, buyPrice = null;

            for (Component component : tooltip) {
                Matcher matcher = pricePattern.matcher(component.getString());
                if (!matcher.find()) continue;

                float price = Float.parseFloat(matcher.group(2));
                if (matcher.group(1).equals("Sell")) {
                    sellPrice = price;
                } else {
                    buyPrice = price;
                }
            }

            if (sellPrice == null && buyPrice == null) continue;

            itemsForSale.put(itemName, new ItemForSale(
                sellPrice == null ? 0 : sellPrice,
                buyPrice == null ? 0 : buyPrice
            ));

            info("got " + itemName + ", B: " + buyPrice + ", S: " + sellPrice);
        }
    }

    public record ItemForSale(float sellPrice, float buyPrice) {
    }
}
