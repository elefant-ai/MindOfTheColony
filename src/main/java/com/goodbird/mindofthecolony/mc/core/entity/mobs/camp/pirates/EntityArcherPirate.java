package com.goodbird.mindofthecolony.mc.core.entity.mobs.camp.pirates;

import com.goodbird.mindofthecolony.mc.api.entity.mobs.pirates.AbstractEntityPirate;
import com.goodbird.mindofthecolony.mc.api.entity.mobs.pirates.AbstractEntityPirateRaider;
import com.goodbird.mindofthecolony.mc.api.entity.mobs.pirates.IArcherPirateEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Class for the Archer Pirate entity.
 */
public class EntityArcherPirate extends AbstractEntityPirate implements IArcherPirateEntity
{
    /**
     * Constructor of the entity.
     *
     * @param worldIn world to construct it in.
     * @param type    the entity type.
     */
    public EntityArcherPirate(final EntityType<? extends EntityArcherPirate> type, final Level worldIn)
    {
        super(type, worldIn);
    }
}
