package org.squiddev.cobalt.function;

import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.UnwindThrowable;
import org.squiddev.cobalt.debug.DebugFrame;

@FunctionalInterface
public interface UnwindableCallable {
    EvalCont call(DebugFrame di) throws LuaError, UnwindThrowable;
}
