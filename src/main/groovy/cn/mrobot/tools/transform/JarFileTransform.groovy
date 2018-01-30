package cn.mrobot.tools.transform

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.google.common.collect.ImmutableSet
import groovy.io.FileType
import org.gradle.api.Project

/**
 * jar包去重
 *
 * @author by devil on 2017/8/1
 * @version 1.0.0
 * @since 1.0.0
 */
class JarFileTransform extends Transform {

    private static final String TRANSFORM_NAME = "JarFile"


    private Project mProject

    JarFileTransform(Project project) {
        mProject = project;
    }

    @Override
    public String getName() {
        return TRANSFORM_NAME
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    public Set<QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT

    }

    @Override
    public boolean isIncremental() {
        return false;
    }

    @Override
    public void transform(Context context, Collection<TransformInput> inputs,
                          Collection<TransformInput> referencedInputs, TransformOutputProvider outputProvider,
                          boolean isIncremental) throws IOException, TransformException, InterruptedException {
        List<String> normalJarOutputs = new ArrayList<>()
        inputs.each {
            it.directoryInputs.each { directoryInput ->
                File dest = outputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY);
                mProject.copy {
                    from mProject.fileTree(directoryInput.file)
                    into dest
                }
            }

            List<File> outputFiles = new ArrayList<>()
            it.jarInputs.each { jarInput ->
                File jarInputFile = jarInput.file
                if (jarInputFile.name == 'classes.jar' || jarInputFile.name.contains('internal_impl')) {
                    File dest = outputProvider.getContentLocation(jarInput.name, jarInput.contentTypes, ImmutableSet.<QualifiedContent.Scope> of(QualifiedContent.Scope.PROJECT_LOCAL_DEPS), Format.DIRECTORY)
                    outputFiles.add(dest)
                    mProject.copy {
                        from mProject.zipTree(jarInputFile)
                        into dest
                    }
                } else {
                    if (!normalJarOutputs.contains(jarInput.file.name)) {
                        normalJarOutputs.add(jarInput.file.name)
                        File dest = outputProvider.getContentLocation(jarInput.name, jarInput.contentTypes, jarInput.scopes, Format.JAR);
                        mProject.copy {
                            from jarInput.file
                            into dest.parent
                        }
                    }else{
                        println 'exists jar : ' + jarInput.file.absolutePath
                    }
                }
            }

            List<String> rFileList = new ArrayList<>()
            outputFiles.each { outputFile ->
                outputFile.traverse(
                        type: FileType.FILES
//                        nameFilter: ~/((^R)|(^R\$)).*\.class/
                ) { file ->
                    def filePathName = file.absolutePath.replace(outputFile.absolutePath, '')
                    if (!rFileList.contains(filePathName)) {
                        rFileList.add(filePathName)
                    } else {
                        println 'delete: ' + file.name
                        file.delete()
                    }
                }
            }
        }
    }
}
