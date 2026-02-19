package com.goodbird.mindofthecolony.mc.api.colony.requestsystem.resolver;

import com.goodbird.mindofthecolony.mc.api.colony.requestsystem.factory.IFactory;
import com.goodbird.mindofthecolony.mc.api.colony.requestsystem.location.ILocation;

/**
 * Interface describing an object that is capable of constructing a specific {@link IRequestResolver}
 *
 * @param <Resolver> The type of {@link IRequestResolver} this factory can produce.
 */
public interface IRequestResolverFactory<Resolver extends IRequestResolver<?>> extends IFactory<ILocation, Resolver>
{
}
