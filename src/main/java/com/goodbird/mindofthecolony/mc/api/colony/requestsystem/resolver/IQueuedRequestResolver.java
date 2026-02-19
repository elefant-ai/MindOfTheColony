package com.goodbird.mindofthecolony.mc.api.colony.requestsystem.resolver;

import com.google.common.collect.ImmutableList;
import com.goodbird.mindofthecolony.mc.api.colony.requestsystem.requestable.IRequestable;
import com.goodbird.mindofthecolony.mc.api.colony.requestsystem.token.IToken;

public interface IQueuedRequestResolver<R extends IRequestable> extends IRequestResolver<R>
{
    /**
     * Method to get a list of all assigned tokens to this resolver.
     *
     * @return A list of all assigned tokens.
     */
    ImmutableList<IToken<?>> getAllAssignedRequests();

    /**
     * Called when the request system this is part of gets reset.
     */
    void onSystemReset();
}
