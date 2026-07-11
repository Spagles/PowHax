package powie.powhax.modules;

import meteordevelopment.meteorclient.events.game.ItemStackTooltipEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.chat.Component;
import powie.powhax.Powhax;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PriceScraper extends Module {
    private static final  Pattern pricePattern = Pattern.compile("(Sell|Buy): (\\d+\\.\\d+)");
    private final Map<String, itemForSale> items = new HashMap<>();

    public PriceScraper() {
        super(Powhax.CATEGORY, "price-scraper", "An example module that highlights the center of the world.");
    }

    @Override
    public void onDeactivate() {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, itemForSale> entry : items.entrySet()) {
            if (!first) json.append(",");
            json.append("\"").append(entry.getKey()).append("\":");
            json.append("{\"sellPrice\":").append(entry.getValue().sellPrice);
            json.append(",\"buyPrice\":").append(entry.getValue().buyPrice).append("}");
            first = false;
        }
        json.append("}");
        info(json.toString());
        items.clear();
    }

    @EventHandler
    private void onItemStackTooltip(ItemStackTooltipEvent event) {
        List<Component> itemTooltips = event.list();

        boolean hasSell = false;
        boolean hasBuy = false;
        float sellPrice = 0;
        float buyPrice = 0;

        for (Component component : itemTooltips) {
            String text = component.getString();
            Matcher matcher = pricePattern.matcher(text);
            if (matcher.find()) {
                String type = matcher.group(1);
                float price = Float.parseFloat(matcher.group(2));
                if (type.equals("Sell")) {
                    hasSell = true;
                    sellPrice = price;
                } else if (type.equals("Buy")) {
                    hasBuy = true;
                    buyPrice = price;
                }
            }
        }

        if (!hasSell && !hasBuy) return;

        String itemName = event.itemStack().getItem().toString();


        if (items.containsKey(itemName)) return;

        items.put(itemName, new itemForSale(sellPrice, buyPrice));

        info("got " + itemName + ", B: " + buyPrice + ", S: " + sellPrice);
    }


    private class itemForSale {
        float sellPrice;
        float buyPrice;

        public itemForSale(float sellPrice, float buyPrice) {
            this.sellPrice = sellPrice;
            this.buyPrice = buyPrice;
        }
    }
}
