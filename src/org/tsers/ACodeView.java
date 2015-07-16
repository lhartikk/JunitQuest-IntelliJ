package org.tsers;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diff.DiffContent;
import com.intellij.openapi.diff.DiffManager;
import com.intellij.openapi.diff.DiffRequest;
import com.intellij.openapi.diff.SimpleContent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.ui.PopupHandler;

import javax.swing.*;
import java.awt.*;


public class ACodeView extends SimpleToolWindowPanel implements Disposable {
    protected final Project project;

    protected final ToolWindowManager toolWindowManager;
    protected final KeymapManager keymapManager;
    private final String extension;


    protected Editor editor;
    protected Document document;

    public ACodeView(final ToolWindowManager toolWindowManager, KeymapManager keymapManager, final Project project, final String fileExtension) {
        super(true, true);
        this.toolWindowManager = toolWindowManager;
        this.keymapManager = keymapManager;
        this.project = project;
        this.extension = fileExtension;
        setupUI();
    }

    public ACodeView(final ToolWindowManager toolWindowManager, KeymapManager keymapManager, final Project project) {
        this(toolWindowManager, keymapManager, project, "java");
    }

    private void setupUI() {
        final EditorFactory editorFactory = EditorFactory.getInstance();
        document = editorFactory.createDocument("");
        editor = editorFactory.createEditor(document, project, FileTypeManager.getInstance().getFileTypeByExtension(extension), true);

        final JComponent editorComponent = editor.getComponent();
        add(editorComponent);
         DefaultActionGroup group = new DefaultActionGroup();

        final ActionManager actionManager = ActionManager.getInstance();
        final ActionToolbar actionToolBar = actionManager.createActionToolbar("JunitQuest", group, true);
        final JPanel buttonsPanel = new JPanel(new BorderLayout());
        buttonsPanel.add(actionToolBar.getComponent(), BorderLayout.CENTER);
        PopupHandler.installPopupHandler(editor.getContentComponent(), group, "JunitQuest", actionManager);
        setToolbar(buttonsPanel);
    }

    public void setCode(final VirtualFile file, final String code) {
        document.setText(code);
     }



    public void dispose() {
        if (editor != null) {
            final EditorFactory editorFactory = EditorFactory.getInstance();
            editorFactory.releaseEditor(editor);
            editor = null;
        }
    }
}
