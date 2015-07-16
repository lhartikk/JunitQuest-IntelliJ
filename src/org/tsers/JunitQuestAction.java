package org.tsers;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import org.tsers.junitquest.TestGenerator;

import java.io.File;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.Semaphore;

public class JunitQuestAction extends AnAction {

    @Override
    public void update(AnActionEvent e) {
        final VirtualFile virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        final Project project = e.getData(PlatformDataKeys.PROJECT);
        final Presentation presentation = e.getPresentation();
        if (project == null || virtualFile == null) {
            presentation.setEnabled(false);
            return;
        }
        final PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        presentation.setEnabled(psiFile instanceof PsiClassOwner);
        super.update(e);
    }

    public void actionPerformed(AnActionEvent e) {
        final VirtualFile virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        final Project project = e.getData(PlatformDataKeys.PROJECT);
        if (project == null || virtualFile == null) return;
        final PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        if (psiFile instanceof PsiClassOwner) {
            Object asdf2 = ((PsiClassOwner) psiFile).getClasses()[0].getName();
            String packageName = ((PsiClassOwner) psiFile).getPackageName();

            String fullName = packageName + "." + asdf2;
            final Module module = ModuleUtil.findModuleForPsiElement(psiFile);
            final CompilerModuleExtension cme = CompilerModuleExtension.getInstance(module);
            final CompilerManager compilerManager = CompilerManager.getInstance(project);
            final VirtualFile[] files = {virtualFile};
            final VirtualFile[] outputDirectories = cme == null ? null : cme.getOutputRoots(true);
            if ("class".equals(virtualFile.getExtension())) {
                updateToolWindowContents(project, virtualFile, outputDirectories, fullName);
            } else if (!virtualFile.isInLocalFileSystem() && !virtualFile.isWritable()) {
                final PsiClass[] psiClasses = ((PsiClassOwner) psiFile).getClasses();
                if (psiClasses.length > 0) {
                    updateToolWindowContents(project, psiClasses[0].getOriginalElement().getContainingFile().getVirtualFile(), outputDirectories, fullName);
                }
            } else {
                final Application application = ApplicationManager.getApplication();
                application.runWriteAction(new Runnable() {
                    public void run() {
                        FileDocumentManager.getInstance().saveAllDocuments();
                    }
                });
                application.executeOnPooledThread(new Runnable() {
                    public void run() {
                        final CompileScope compileScope = compilerManager.createFilesCompileScope(files);
                        final VirtualFile[] result = {null};
                        final VirtualFile[] outputDirectories = cme == null ? null : cme.getOutputRoots(true);
                        final Semaphore semaphore = new Semaphore(1);
                        try {
                            semaphore.acquire();
                        } catch (InterruptedException e1) {
                            result[0] = null;
                        }
                        if (outputDirectories != null && compilerManager.isUpToDate(compileScope)) {
                            application.invokeLater(new Runnable() {
                                public void run() {
                                    result[0] = findClassFile(outputDirectories, psiFile);
                                    semaphore.release();
                                }
                            });
                        } else {
                            application.invokeLater(new Runnable() {
                                public void run() {
                                    compilerManager.compile(files, new CompileStatusNotification() {
                                        public void finished(boolean aborted, int errors, int warnings, final CompileContext compileContext) {
                                            if (errors == 0) {
                                                VirtualFile[] outputDirectories = cme.getOutputRoots(true);
                                                if (outputDirectories != null) {
                                                    result[0] = findClassFile(outputDirectories, psiFile);
                                                }
                                            }
                                            semaphore.release();
                                        }
                                    });
                                }
                            });
                            try {
                                semaphore.acquire();
                            } catch (InterruptedException e1) {
                                result[0] = null;
                            }
                        }
                        application.invokeLater(new Runnable() {
                            public void run() {
                                updateToolWindowContents(project, result[0], outputDirectories, fullName);
                            }
                        });
                    }
                });
            }
        }
    }

    private VirtualFile findClassFile(final VirtualFile[] outputDirectories, final PsiFile psiFile) {
        return ApplicationManager.getApplication().runReadAction(new Computable<VirtualFile>() {
            public VirtualFile compute() {
                if (outputDirectories != null && psiFile instanceof PsiClassOwner) {
                    FileEditor editor = FileEditorManager.getInstance(psiFile.getProject()).getSelectedEditor(psiFile.getVirtualFile());
                    int caretOffset = editor == null ? -1 : ((PsiAwareTextEditorImpl) editor).getEditor().getCaretModel().getOffset();
                    if (caretOffset >= 0) {
                        PsiClass psiClass = findClassAtCaret(psiFile, caretOffset);
                        if (psiClass != null) {
                            return getClassFile(psiClass);
                        }
                    }
                    PsiClassOwner psiJavaFile = (PsiClassOwner) psiFile;
                    for (PsiClass psiClass : psiJavaFile.getClasses()) {
                        final VirtualFile file = getClassFile(psiClass);
                        if (file != null) {
                            return file;
                        }
                    }
                }
                return null;
            }

            private VirtualFile getClassFile(PsiClass psiClass) {
                StringBuilder sb = new StringBuilder(psiClass.getQualifiedName());
                while (psiClass.getContainingClass() != null) {
                    sb.setCharAt(sb.lastIndexOf("."), '$');
                    psiClass = psiClass.getContainingClass();
                }
                String classFileName = sb.toString().replace('.', '/') + ".class";
                for (VirtualFile outputDirectory : outputDirectories) {
                    final VirtualFile file = outputDirectory.findFileByRelativePath(classFileName);
                    if (file != null && file.exists()) {
                        return file;
                    }
                }
                return null;
            }

            private PsiClass findClassAtCaret(PsiFile psiFile, int caretOffset) {
                PsiElement elem = psiFile.findElementAt(caretOffset);
                while (elem != null) {
                    if (elem instanceof PsiClass) {
                        return (PsiClass) elem;
                    }
                    elem = elem.getParent();
                }
                return null;
            }
        });
    }

    private String getTextToShow(Optional<VirtualFile> outputVirtualFile, String className) {
        if (!outputVirtualFile.isPresent()) {
            return "Cannot find bytecode directory. Please compile the project first.";
        } else {
            String bytecodePath = outputVirtualFile.get().getPath();
            TestGenerator tg = new TestGenerator();
            return getCode(bytecodePath, className, tg);
        }
    }

    private void updateToolWindowContents(final Project project, final VirtualFile file, VirtualFile[] outputDirectories, final String fullname) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            public void run() {
                Optional<VirtualFile> outputVirtualFile = getOutPutdirectory(outputDirectories, file);
                String text = getTextToShow(outputVirtualFile, fullname);

                final JunitQuestOutline junitQuestOutline = JunitQuestOutline.getInstance(project);

                PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText("test.java", text);
                CodeStyleManager.getInstance(project).reformat(psiFile);
                junitQuestOutline.setCode(file, psiFile.getText());
                ToolWindowManager.getInstance(project).getToolWindow("JunitQuest").activate(null);
            }


        });
    }

    private String getCode(String outputPath, String className, TestGenerator tg) {
        String code;
        Optional<String> runtimeLocation = findRuntimeJarLocation();
        if (runtimeLocation.isPresent()) {
            code = tg.generateTests(className, outputPath, getClass().getClassLoader(), runtimeLocation.get());
        } else {
            code = tg.generateTests(className, outputPath, getClass().getClassLoader());
        }
        return code;
    }

    private static Optional<VirtualFile> getOutPutdirectory(VirtualFile outputDirectories[], VirtualFile file) {
        if(file == null) {
            return Optional.empty();
        }
        for (int i = 0; i < outputDirectories.length; i++) {
            VirtualFile outputDir = outputDirectories[i];
            if (file.getPath().contains(outputDir.getPath())) {
                return Optional.of(outputDir);
            }
        }
        return Optional.empty();
    }

    private static Optional<String> findRuntimeJarLocation() {
        String classPath = System.getProperty("sun.boot.class.path");

        return Arrays.asList(classPath.split(File.pathSeparator)).stream()
                .filter(c -> c.endsWith(File.separatorChar +"rt.jar"))
                .findFirst();

    }
}
