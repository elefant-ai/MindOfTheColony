package com.goodbird.mindofthecolony.mc.core.entity.mobs.raider.barbarians;

import com.goodbird.mindofthecolony.mc.api.entity.mobs.barbarians.AbstractEntityBarbarianRaider;
import com.goodbird.mindofthecolony.mc.api.entity.mobs.barbarians.IMeleeBarbarianEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Class for the Barbarian entity.
 */
public class EntityBarbarianRaider extends AbstractEntityBarbarianRaider implements IMeleeBarbarianEntity
{

    /**
     * Constructor of the entity.
     *
     * @param worldIn world to construct it in.
     * @param type    the entity type.
     */
    public EntityBarbarianRaider(final EntityType<? extends EntityBarbarianRaider> type, final Level worldIn)
    {
        super(type, worldIn);
    }
}
