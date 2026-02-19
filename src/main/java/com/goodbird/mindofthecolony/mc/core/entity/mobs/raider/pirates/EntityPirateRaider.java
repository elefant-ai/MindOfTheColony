package com.goodbird.mindofthecolony.mc.core.entity.mobs.raider.pirates;

import com.goodbird.mindofthecolony.mc.api.entity.mobs.pirates.AbstractEntityPirateRaider;
import com.goodbird.mindofthecolony.mc.api.entity.mobs.pirates.IMeleePirateEntity;
import com.goodbird.mindofthecolony.mc.core.entity.pathfinding.navigation.MovementHandler;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Class for the Pirate entity.
 */
public class EntityPirateRaider extends AbstractEntityPirateRaider implements IMeleePirateEntity
{

    /**
     * Constructor of the entity.
     *
     * @param type    the entity type.
     * @param worldIn world to construct it in.
     */
    public EntityPirateRaider(final EntityType<? extends EntityPirateRaider> type, final Level worldIn)
    {
        super(type, worldIn);
        this.moveControl = new MovementHandler(this);
    }
}
