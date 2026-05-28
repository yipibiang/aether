package com.aether.foundation.tools;

import java.util.List;

/** Port for discovering tools available to an agent (implementations live outside foundation). */
public interface ToolRegistry {

    List<Tool> tools();
}
