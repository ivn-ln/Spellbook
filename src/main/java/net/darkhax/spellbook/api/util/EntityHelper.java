package net.darkhax.spellbook.api.util;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class EntityHelper {

    public static boolean isCrouching(ComponentAccessor<EntityStore> components, Ref<EntityStore> target) {
        final MovementStatesComponent movement = components.getComponent(target, MovementStatesComponent.getComponentType());
        return movement != null && movement.getMovementStates().crouching;
    }
}
