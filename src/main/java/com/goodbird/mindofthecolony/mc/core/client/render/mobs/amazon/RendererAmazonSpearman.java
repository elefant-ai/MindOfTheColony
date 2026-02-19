package com.goodbird.mindofthecolony.mc.core.client.render.mobs.amazon;

import com.goodbird.mindofthecolony.mc.api.entity.mobs.AbstractEntityMinecoloniesMonster;
import com.goodbird.mindofthecolony.mc.core.client.model.raiders.ModelAmazonSpearman;
import com.goodbird.mindofthecolony.mc.core.event.ClientRegistryHandler;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * Renderer used for spearman amazons.
 */
public class RendererAmazonSpearman extends AbstractRendererAmazon<AbstractEntityMinecoloniesMonster, ModelAmazonSpearman>
{
    /**
     * Texture of the entity.
     */
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecolonies", "textures/entity/raiders/amazon_spearman.png");

    /**
     * Constructor method for renderer
     *
     * @param context the renderManager
     */
    public RendererAmazonSpearman(final EntityRendererProvider.Context context)
    {
        super(context, new ModelAmazonSpearman(context.bakeLayer(ClientRegistryHandler.AMAZON_SPEARMAN)), 0.5F);
    }

    @NotNull
    @Override
    public ResourceLocation getTextureLocation(@NotNull final AbstractEntityMinecoloniesMonster entity)
    {
        return TEXTURE;
    }
}
