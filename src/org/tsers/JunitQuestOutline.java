package org.tsers;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowManager;


public class JunitQuestOutline extends ACodeView {
    public JunitQuestOutline(final Project project, KeymapManager keymapManager, final ToolWindowManager toolWindowManager) {
        super(toolWindowManager, keymapManager, project);
    }

    public static JunitQuestOutline getInstance(Project project) {
        return ServiceManager.getService(project, JunitQuestOutline.class);
    }
}
