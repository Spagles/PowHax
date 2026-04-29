package powie.powhax.modules;

import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import powie.powhax.Powhax;

public class FlySpeed extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    /**
     * Example setting.
     * The {@code name} parameter should be in kebab-case.
     * If you want to access the setting from another class, simply make the setting {@code public}, and use
     * {@link meteordevelopment.meteorclient.systems.modules.Modules#get(Class)} to access the {@link Module} object.
     */
    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("Speed")
        .description("how fast u wanna go?")
        .defaultValue(0.250)
        .max(10)
        .min(0)
        .build()
    );


    public FlySpeed() {
        super(Powhax.CATEGORY, "Fly-Speed", "Sets the flight speed for abilities");
    }

    @Override
    public void onActivate() {
        mc.player.getAbilities().setFlyingSpeed(speed.get().floatValue());
    }

    @Override
    public void onDeactivate() {
        mc.player.getAbilities().setFlyingSpeed(0.05f);
    }
}
