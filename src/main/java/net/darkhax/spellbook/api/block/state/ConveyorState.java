package net.darkhax.spellbook.api.block.state;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.collision.CollisionModule;
import com.hypixel.hytale.server.core.modules.collision.CollisionResult;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.state.TickableBlockState;
import com.hypixel.hytale.server.core.universe.world.connectedblocks.ConnectedBlockPatternRule.AdjacentSide;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.darkhax.spellbook.api.codec.Codecs;
import net.darkhax.spellbook.api.util.EntityHelper;
import net.darkhax.spellbook.api.util.WorldHelper;

import javax.annotation.Nonnull;
import java.util.List;

public class ConveyorState extends BlockState implements TickableBlockState {

    public static final BuilderCodec<ConveyorState> CODEC = BuilderCodec.builder(ConveyorState.class, ConveyorState::new, BlockState.BASE_CODEC).build();
    protected Data data;

    @Override
    public boolean initialize(BlockType blockType) {
        if (super.initialize(blockType) && blockType.getState() instanceof Data data) {
            this.data = data;
            return true;
        }
        return false;
    }

    @Override
    public void tick(float dt, int index, ArchetypeChunk<ChunkStore> archeChunk, Store<ChunkStore> store, CommandBuffer<ChunkStore> commandBuffer) {
        final World world = store.getExternalData().getWorld();
        final Store<EntityStore> entities = world.getEntityStore().getStore();
        final Vector3i directionOffset = WorldHelper.rotate(data.direction, this.getRotationIndex()).relativePosition;
        final Vector3d forceVec = new Vector3d((directionOffset.x * 1.5f) * data.force, (directionOffset.y * 1.5f) * data.force, (directionOffset.z * 1.5f) * data.force);
        for (Ref<EntityStore> target : getAllEntitiesInBox(this.getBlockPosition(), data.height, entities, data.players, data.entities, data.items)) {
            final Player player = entities.getComponent(target, Player.getComponentType());
            if (player != null) {
                if (!EntityHelper.isCrouching(entities, target)) {
                    final Velocity velocity = entities.getComponent(target, Velocity.getComponentType());
                    if (velocity != null) {
                        velocity.addInstruction(forceVec.clone().scale(12), null, ChangeVelocityType.Set);
                    }
                }
            }
            else {
                final TransformComponent targetPos = entities.getComponent(target, TransformComponent.getComponentType());
                if (targetPos != null) {
                    final CollisionResult collision = new CollisionResult();
                    final Box boundingBox = entities.getComponent(target, BoundingBox.getComponentType()).getBoundingBox();
                    CollisionModule.findCollisions(boundingBox, targetPos.getPosition().clone(), forceVec.clone(), collision, entities);
                    if (collision.getFirstBlockCollision() == null) {
                        targetPos.getPosition().assign(targetPos.getPosition().add(forceVec.clone()));
                    }
                }
            }
        }
    }

    @Nonnull
    public static List<Ref<EntityStore>> getAllEntitiesInBox(Vector3i pos, float height, @Nonnull ComponentAccessor<EntityStore> components, boolean players, boolean entities, boolean items) {
        final ObjectList<Ref<EntityStore>> results = SpatialResource.getThreadLocalReferenceList();
        final Vector3d min = new Vector3d(pos.x, pos.y, pos.z);
        final Vector3d max = new Vector3d(pos.x + 1, pos.y + height, pos.z + 1);
        if (entities) {
            components.getResource(EntityModule.get().getEntitySpatialResourceType()).getSpatialStructure().collectBox(min, max, results);
        }
        if (players) {
            components.getResource(EntityModule.get().getPlayerSpatialResourceType()).getSpatialStructure().collectBox(min, max, results);
        }
        if (items) {
            components.getResource(EntityModule.get().getItemSpatialResourceType()).getSpatialStructure().collectBox(min, max, results);
        }
        return results;
    }

    public static class Data extends StateData {

        @Nonnull
        public static final BuilderCodec<Data> CODEC = BuilderCodec.builder(Data.class, Data::new, StateData.DEFAULT_CODEC)
                .documentation("Pushes the mob in a given direction.")
                .append(new KeyedCodec<>("Direction", Codecs.SIDE), (o, v) -> o.direction = v, o -> o.direction)
                .documentation("The direction to push mobs.")
                .add()
                .append(new KeyedCodec<>("Force", Codec.FLOAT), (o, v) -> o.force = v, o -> o.force)
                .documentation("How much force to push the mob with.")
                .add()
                .append(new KeyedCodec<>("Height", Codec.FLOAT), (o, v) -> o.height = v, o -> o.height)
                .documentation("How high should the conveyor search?")
                .add()
                .append(new KeyedCodec<>("Players", Codec.BOOLEAN), (o, v) -> o.players = v, o -> o.players)
                .documentation("Should players be affected?")
                .add()
                .append(new KeyedCodec<>("Items", Codec.BOOLEAN), (o, v) -> o.items = v, o -> o.items)
                .documentation("Should items be affected?")
                .add()
                .append(new KeyedCodec<>("Entities", Codec.BOOLEAN), (o, v) -> o.entities = v, o -> o.entities)
                .documentation("Should entities be affected?")
                .add()
                .build();

        private AdjacentSide direction = AdjacentSide.North;
        private float force = 1f;
        private boolean players = true;
        private boolean entities = true;
        private boolean items = true;
        private float height = 0.99f;
    }
}