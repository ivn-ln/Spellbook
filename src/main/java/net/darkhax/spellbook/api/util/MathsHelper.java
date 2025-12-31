package net.darkhax.spellbook.api.util;

import com.hypixel.hytale.protocol.Rangef;

import java.util.Random;

public class MathsHelper {

    public static final Random RNG = new Random();

    public static int fromRange(Rangef range) {
        int low = (int) Math.ceil(range.min);
        int high = (int) Math.floor(range.max);
        return low + RNG.nextInt(high - low + 1);
    }
}