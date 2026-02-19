package com.goodbird.mindofthecolony.mc.apiimp;

import com.goodbird.mindofthecolony.mc.api.client.render.modeltype.registry.IModelTypeRegistry;
import com.goodbird.mindofthecolony.mc.core.client.render.modeltype.registry.ModelTypeRegistry;
import net.neoforged.neoforge.registries.NewRegistryEvent;

public class ClientMinecoloniesAPIImpl extends CommonMinecoloniesAPIImpl
{
    private final IModelTypeRegistry modelTypeRegistry = new ModelTypeRegistry();

    @Override
    public IModelTypeRegistry getModelTypeRegistry()
    {
        return modelTypeRegistry;
    }

    @Override
    public void onRegistryNewRegistry(final NewRegistryEvent event)
    {
        super.onRegistryNewRegistry(event);
    }
}
