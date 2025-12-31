package net.darkhax.spellbook.api.codec.output;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.protocol.Rangef;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemDropList;
import com.hypixel.hytale.server.core.codec.ProtocolCodecs;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.item.ItemModule;
import net.darkhax.spellbook.api.util.MathsHelper;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;

/**
 * Outputs an item using an Item ID and a quantity.
 * <pre><code>
 * {
 *   "Type": "DropList",
 *   "DropList": "Drop_Cow",
 *   "Rolls": {
 *     "Min": 1,
 *     "Max": 3
 *   }
 * }
 * </code></pre>
 */
public class DropListOutput implements ItemOutput {

    public static String ID = "DropList";
    public static BuilderCodec<DropListOutput> CODEC = BuilderCodec.builder(DropListOutput.class, DropListOutput::new)
            .documentation("Outputs items from a drop list.")
            .append(new KeyedCodec<>("DropList", Codec.STRING, true), (i, v) -> i.droplistId = v, i -> i.droplistId)
            .documentation("The ID of the drop list. Same format as the vanilla droplist command.")
            .addValidator(Validators.nonNull())
            .addValidator(ItemDropList.VALIDATOR_CACHE.getValidator())
            .add()
            .append(new KeyedCodec<>("Rolls", ProtocolCodecs.RANGEF), (i, v) -> i.count = v, i -> i.count)
            .documentation("The amount of times to drop the loot. Defaults to 1.")
            .add()
            .build();

    @Override
    public String toString() {
        return "DropListOutput{" + "droplistId='" + droplistId + '\'' + ", count=" + count + '}';
    }

    @Nonnull
    protected String droplistId = "Drops_Plant_Crop_Wheat_StageFinal_Harvest";
    protected Rangef count = new Rangef(1, 1);

    public String dropListId() {
        return this.droplistId;
    }

    public Rangef rolls() {
        return this.count;
    }

    @Override
    public void output(Consumer<ItemStack> consumer) {
        final ItemDropList dropList = ItemDropList.getAssetMap().getAsset(this.droplistId);
        if (dropList != null) {
            final int rolls = MathsHelper.fromRange(count);
            for (int roll = 0; roll < rolls; roll++) {
                final List<ItemStack> drops = ItemModule.get().getRandomItemDrops(droplistId);
                for (ItemStack stack : drops) {
                    if (stack != null && !stack.isEmpty()) {
                        consumer.accept(stack);
                    }
                }
            }
        }
    }
}