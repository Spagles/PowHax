package powie.powhax.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import powie.powhax.Powhax;

import java.util.ArrayList;
import java.util.List;

public class BlazeFarm extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Weapon> weapon = sgGeneral.add(new EnumSetting.Builder<Weapon>()
        .name("weapon")
        .description("Only attacks an entity when a specified weapon is in your hand.")
        .defaultValue(Weapon.Sword)
        .build()
    );

    private final Setting<RotationMode> rotation = sgGeneral.add(new EnumSetting.Builder<RotationMode>()
        .name("rotate")
        .description("Determines when you should rotate towards the target.")
        .defaultValue(RotationMode.OnHit)
        .build()
    );

    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
        .name("priority")
        .description("How to filter targets within range.")
        .defaultValue(SortPriority.LowestDistance)
        .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("The maximum range the entity can be to attack it.")
        .defaultValue(4.5)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<Double> wallsRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("walls-range")
        .description("The maximum range the entity can be attacked through walls.")
        .defaultValue(3.5)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<Boolean> pauseOnLag = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-on-lag")
        .description("Pauses if the server is lagging.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> tpsSync = sgGeneral.add(new BoolSetting.Builder()
        .name("TPS-sync")
        .description("Tries to sync attack delay with the server's TPS.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> sellDelay = sgGeneral.add(new IntSetting.Builder()
        .name("sell-delay-interval")
        .description("the delay before selling the blaze rods in minecraft ticks")
        .defaultValue(1200)
        .min(1)
        .sliderMin(1)
        .max(6000)
        .sliderMax(6000)
        .build()
    );

    private final Setting<Boolean> autoFix = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-fix")
        .description("Auto Fixes weapon")
        .defaultValue(true)
        .build()
    );

    private final Setting<String> autoFixCommand = sgGeneral.add(new StringSetting.Builder()
        .name("autoFix-Command")
        .defaultValue("/fix")
        .visible(autoFix::get)
        .build()
    );

    private final Setting<Integer> autoFixDelay = sgGeneral.add(new IntSetting.Builder()
        .name("autoFix-Delay")
        .description("the delay before fixing the weapon in minecraft ticks")
        .defaultValue(5)
        .visible(autoFix::get)
        .min(1)
        .sliderMin(1)
        .max(80)
        .sliderMax(80)
        .build()
    );

    private final List<Entity> targets = new ArrayList<>();
    private int switchTimer, sellTimer, fixTimer;
    public boolean attacking;

    public BlazeFarm() {
        super(Powhax.CATEGORY, "blaze-farm", "Automatically farms Blazes");
    }

    @Override
    public void onDeactivate() {
        targets.clear();
        attacking = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!mc.player.isAlive() || PlayerUtils.getGameMode() == GameType.SPECTATOR) return;
        if (TickRate.INSTANCE.getTimeSinceLastTick() >= 1f && pauseOnLag.get()) return;

        targets.clear();
        TargetUtils.getList(targets, this::entityCheck, priority.get(), 1);

        if (targets.isEmpty()) {
            attacking = false;
            return;
        }

        Entity primary = targets.getFirst();

        if (!itemInHand()) return;
        attacking = true;
        if (rotation.get() == RotationMode.Always) {
            Rotations.rotate(Rotations.getYaw(primary), Rotations.getPitch(primary, Target.Body));
        }
        if (delayCheck()) targets.forEach(this::attack);

        if (autoFix.get()) fix();

        if (sellTimer <= sellDelay.get()) {
            sellTimer++;
            return;
        }
        FindItemResult item = InvUtils.findInHotbar(Items.BLAZE_ROD);
        if (!item.found()) {
            if (20 <= sellDelay.get()) info("Blaze rod not found in hotbar");
            if (600 >= sellDelay.get()) {
                sellTimer = 0; // if sellDelay is lower than 30 seconds, then reset sellTimer(tick)
                return;
            }
        }
        int prevSlot = mc.player.getInventory().getSelectedSlot();
        InvUtils.swap(item.slot(), false);
        ChatUtils.sendPlayerMsg("/sell handall");
        InvUtils.swap(prevSlot, false);
        sellTimer = 0;
    }

    private boolean entityCheck(Entity entity) {
        if (entity.equals(mc.player) || entity.equals(mc.getCameraEntity())) return false;
        if ((entity instanceof LivingEntity livingEntity && livingEntity.isDeadOrDying()) || !entity.isAlive())
            return false;

        AABB hitbox = entity.getBoundingBox();
        if (!PlayerUtils.isWithin(
            Mth.clamp(mc.player.getX(), hitbox.minX, hitbox.maxX),
            Mth.clamp(mc.player.getY(), hitbox.minY, hitbox.maxY),
            Mth.clamp(mc.player.getZ(), hitbox.minZ, hitbox.maxZ),
            range.get()
        )) return false;
        if (entity.getType() != EntityType.BLAZE) return false;
        return PlayerUtils.canSeeEntity(entity) || PlayerUtils.isWithin(entity, wallsRange.get());
    }

    private boolean delayCheck() {
        if (switchTimer > 0) {
            switchTimer--;
            return false;
        }

        float delay = 0.5f;
        if (tpsSync.get()) delay /= (TickRate.INSTANCE.getTickRate() / 20);


        return mc.player.getAttackStrengthScale(delay) >= 1;
    }

    private void attack(Entity target) {
        if (rotation.get() == RotationMode.OnHit) {
            Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target, Target.Body));
        }

        mc.gameMode.attack(mc.player, target);
        mc.player.swing(InteractionHand.MAIN_HAND);
    }

    private void fix() {
        if (autoFixDelay.get() >= fixTimer) {
            fixTimer++;
        } else {
            fixTimer = 0;
            if (mc.player.getMainHandItem().getDamageValue() <= 0) return;
            ChatUtils.sendPlayerMsg(autoFixCommand.get());
        }
    }

    private boolean itemInHand() {
        return switch (weapon.get()) {
            case Axe -> mc.player.getMainHandItem().getItem() instanceof AxeItem;
            case Sword -> mc.player.getMainHandItem().is(ItemTags.SWORDS);
            case Both ->
                mc.player.getMainHandItem().getItem() instanceof AxeItem || mc.player.getMainHandItem().is(ItemTags.SWORDS);
            default -> true;
        };
    }

    public Entity getTarget() {
        if (!targets.isEmpty()) return targets.getFirst();
        return null;
    }

    @Override
    public String getInfoString() {
        if (!targets.isEmpty()) return EntityUtils.getName(getTarget());
        return null;
    }

    public enum Weapon {
        Sword,
        Axe,
        Both,
        Any
    }

    public enum RotationMode {
        Always,
        OnHit,
        None
    }

}
