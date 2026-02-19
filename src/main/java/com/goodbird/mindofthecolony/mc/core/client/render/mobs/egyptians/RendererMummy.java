package com.goodbird.mindofthecolony.mc.core.client.render.mobs.egyptians;

import com.goodbird.mindofthecolony.mc.api.entity.mobs.AbstractEntityMinecoloniesMonster;
import com.goodbird.mindofthecolony.mc.core.client.model.raiders.ModelMummy;
import com.goodbird.mindofthecolony.mc.core.event.ClientRegistryHandler;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer used for mummies.
 */
public class RendererMummy extends AbstractRendererEgyptian<AbstractEntityMinecoloniesMonster, ModelMummy>
{
    /**
     * Texture of the entity.
     */
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecolonies", "textures/entity/raiders/mummy.png");

    /**
     * Constructor method for renderer
     *
     * @param context the renderManager
     */
    public RendererMummy(final EntityRendererProvider.Context context)
    {
        super(context, new ModelMummy(context.bakeLayer(ClientRegistryHandler.MUMMY)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(final AbstractEntityMinecoloniesMonster entity)
    {
        return TEXTURE;
    }
}
