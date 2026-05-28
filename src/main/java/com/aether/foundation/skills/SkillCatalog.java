package com.aether.foundation.skills;

import java.io.IOException;
import java.util.List;

/** Port for listing available skills; implementations may use the filesystem or remote stores. */
public interface SkillCatalog {

    List<SkillDescriptor> listSkills() throws IOException;
}
