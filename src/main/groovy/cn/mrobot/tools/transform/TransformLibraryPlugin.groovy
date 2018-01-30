package cn.mrobot.tools.transform

import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApplicationVariant
import groovy.io.FileType
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

/**
 *
 *  android aar 内容去重
 *  1. jar file
 *  2. jni file
 *
 * Created by devil on 2017/8/2
 */
class TransformLibraryPlugin implements Plugin<Project> {

    def Project mProject;
    def List<String> resFileList = new ArrayList<>()

    @Override
    void apply(Project project) {
        this.mProject = project
        AppExtension android = project.android
        android.registerTransform(new JarFileTransform(project))
        project.afterEvaluate {
            android.applicationVariants.all { applicationVariant ->
                transformJniFiles(applicationVariant)
                transformResFiles(applicationVariant)
            }
        }
    }

    /**
     * 过滤重复 jni 文件
     * @param variant
     */
    private void transformJniFiles(ApplicationVariant variant) {
        String transformNativeLibs = 'transformNative_libsWithMergeJniLibsFor' + variant.name.capitalize()
        Task transformNativeLibsTask = mProject.tasks.findByPath(transformNativeLibs)
        if (transformNativeLibsTask == null) {
            transformNativeLibs = 'transformNativeLibsWithMergeJniLibsFor' + variant.name.capitalize()
            transformNativeLibsTask = mProject.tasks.findByPath(transformNativeLibs)
        }
        if (transformNativeLibsTask != null) {
            transformNativeLibsTask.doFirst { task ->
                List<String> jniFileList = new ArrayList<>()
                task.inputs.files.each { jniFolder ->
                    if (jniFolder.exists() && jniFolder.isDirectory()) {
                        jniFolder.traverse(
                                type: FileType.FILES,
//                                nameFilter: ~/.*\.so/
                        ) { jniFile ->
                            if (!jniFileList.contains(jniFile.name)) {
                                jniFileList.add(jniFile.name)
                            } else {
                                println 'delete: ' + jniFile.absolutePath
                                jniFile.delete()
                            }

                        }
                    }
                }
            }
        }
    }

    private void transformResFiles(ApplicationVariant variant) {
        String transformResFiles = 'transformResourcesWithMergeJavaResFor' + variant.name.capitalize()
        Task transformResFilesTask = mProject.tasks.findByPath(transformResFiles)
        if (transformResFilesTask != null) {
            transformResFilesTask.doFirst { task ->
                task.inputs.files.each {
                    if (!it.isDirectory()) {
                        checkExist(it)
                    } else {
                        it.traverse { file ->
                            checkExist(file)
                        }
                    }
                }
                resFileList.clear();
            }
        }
    }

    private void checkExist(File file) {
        String fileName = file.name
        if (fileName != 'classes.jar') {
            if (resFileList.contains(fileName)) {
                println 'duplicate file: ' + file.absolutePath
                file.delete()
            } else {
                resFileList.add(fileName)
            }
        }
    }


}
