package io.tpersson.ufw.mediator

public interface Request<TResult>

public interface Query<TResult> : Request<TResult>

public interface Command<TResult> : Request<TResult>