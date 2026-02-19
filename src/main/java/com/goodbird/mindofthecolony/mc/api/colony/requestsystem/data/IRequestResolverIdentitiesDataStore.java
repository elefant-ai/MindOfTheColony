package com.goodbird.mindofthecolony.mc.api.colony.requestsystem.data;

import com.goodbird.mindofthecolony.mc.api.colony.requestsystem.request.IRequest;
import com.goodbird.mindofthecolony.mc.api.colony.requestsystem.resolver.IRequestResolver;
import com.goodbird.mindofthecolony.mc.api.colony.requestsystem.token.IToken;

/**
 * The KV-Store for the requests and their identities. Extends the {@link IIdentitiesDataStore} with {@link IToken} as key type and {@link IRequest} as value type.
 */
public interface IRequestResolverIdentitiesDataStore extends IIdentitiesDataStore<IToken<?>, IRequestResolver<?>>
{
}
