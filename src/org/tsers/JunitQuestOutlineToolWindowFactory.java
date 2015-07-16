package org.tsers;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.ContentFactory;

public class JunitQuestOutlineToolWindowFactory implements ToolWindowFactory {
    public void createToolWindowContent(final Project project, final ToolWindow toolWindow) {
        JunitQuestOutline outline = JunitQuestOutline.getInstance(project);
        toolWindow.getContentManager().addContent(ContentFactory.SERVICE.getInstance().createContent(outline, "", false));

    }
}