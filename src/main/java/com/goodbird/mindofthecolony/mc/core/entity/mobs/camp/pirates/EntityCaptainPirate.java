package com.goodbird.mindofthecolony.mc.core.entity.mobs.camp.pirates;

import com.goodbird.mindofthecolony.mc.api.entity.mobs.pirates.AbstractEntityPirate;
import com.goodbird.mindofthecolony.mc.api.entity.mobs.pirates.ICaptainPirateEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import static com.goodbird.mindofthecolony.mc.api.entity.mobs.RaiderMobUtils.MOB_ATTACK_DAMAGE;

/**
 * Class for the Chief Pirate entity.
 */
public class EntityCaptainPirate extends AbstractEntityPirate implements ICaptainPirateEntity
{

    /**
     * Constructor of the entity.
     *
     * @param type    the entity type.
     * @param worldIn world to construct it in.
     */
    public EntityCaptainPirate(final EntityType<? extends EntityCaptainPirate> type, final Level worldIn)
    {
        super(type, worldIn);
    }

    @Override
    public void initStatsFor(final double baseHealth, final double difficulty, final double baseDamage)
    {
        super.initStatsFor(baseHealth, difficulty, baseDamage);
        this.getAttribute(Attributes.ARMOR).setBaseValue(-1);
        this.getAttribute(MOB_ATTACK_DAMAGE).setBaseValue(baseDamage + 2.0);
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(baseHealth * 1.3);
        this.setHealth(this.getMaxHealth());
    }
}
