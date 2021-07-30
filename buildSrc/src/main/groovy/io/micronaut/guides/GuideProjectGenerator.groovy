package io.micronaut.guides

import groovy.json.JsonSlurper
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import io.micronaut.context.ApplicationContext
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.core.util.StringUtils
import io.micronaut.guides.GuideMetadata.App
import io.micronaut.starter.api.TestFramework
import io.micronaut.starter.application.ApplicationType
import io.micronaut.starter.options.BuildTool
import io.micronaut.starter.options.JdkVersion
import io.micronaut.starter.options.Language
import org.gradle.api.GradleException

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING

@CompileStatic
class GuideProjectGenerator implements Closeable {

    public static final List<JdkVersion> JDK_VERSIONS_SUPPORTED_BY_GRAALVM = Arrays.asList(JdkVersion.JDK_8, JdkVersion.JDK_11)
    public static final String DEFAULT_APP_NAME = 'default'

    private final ApplicationContext applicationContext
    private final GuidesGenerator guidesGenerator

    String basePackage = 'example.micronaut'
    String appName = 'micronautguide'

    GuideProjectGenerator() {
        applicationContext = ApplicationContext.run()
        guidesGenerator = applicationContext.getBean(GuidesGenerator)
    }

    @Override
    void close() throws IOException {
        applicationContext.close()
    }

    @CompileDynamic
    static List<GuideMetadata> parseGuidesMetadata(File guidesFolder,
                                                   String metadataConfigName) {
        List<GuideMetadata> result = []
        guidesFolder.eachDir { dir ->
            result << parseGuideMetadata(dir, metadataConfigName)
        }
        result
    }

    @CompileDynamic
    private static GuideMetadata parseGuideMetadata(File dir, String metadataConfigName) {
        File configFile = new File(dir, metadataConfigName)
        if (!configFile.exists()) {
            throw new GradleException("metadata file not found for ${dir.name}")
        }
        def config = new JsonSlurper().parse(configFile)
        Category cat = Category.values().find {it.toString() == config.category }
        if (!cat) {
            throw new GradleException("$config.category does not exist in Category enum")
        }
        new GuideMetadata(
                asciidoctor: config.asciidoctor,
                slug: config.slug,
                title: config.title,
                intro: config.intro,
                authors: config.authors,
                tags: config.tags,
                category: cat,
                publicationDate: LocalDate.parse(config.publicationDate),
                languages: config.languages ?: ['java', 'groovy', 'kotlin'],
                buildTools: config.buildTools ?: ['gradle', 'maven'],
                testFramework: config.testFramework,
                skipGradleTests: config.skipGradleTests ?: false,
                skipMavenTests: config.skipMavenTests ?: false,
                minimumJavaVersion: config.minimumJavaVersion,
                zipIncludes: config.zipIncludes ?: [],
                apps: config.apps.collect { it -> new App(name: it.name,
                        features: it.features,
                        applicationType: it.applicationType ? ApplicationType.valueOf(it.applicationType.toUpperCase()) : ApplicationType.DEFAULT,
                        excludeSource:  it.excludeSource,
                        excludeTest:  it.excludeTest,
                )
                }
        )
    }

    @CompileDynamic
    void generate(File guidesFolder,
                  File output,
                  String metadataConfigName,
                  File asciidocDir) {

        guidesFolder.eachDir { dir ->
            GuideMetadata metadata = parseGuideMetadata(dir, metadataConfigName)
            if (Utils.process(metadata)) {
                generate(metadata, dir, output)
                GuideAsciidocGenerator.generate(metadata, dir, asciidocDir)
            }
        }
    }

    static String folderName(String slug, GuidesOption guidesOption) {
        "${slug}-${guidesOption.buildTool.toString()}-${guidesOption.language}"
    }

    void generate(GuideMetadata metadata, File inputDir, File outputDir) {
        String packageAndName = "${basePackage}.${appName}"

        List<GuidesOption> guidesOptionList = guidesOptions(metadata)
        JdkVersion javaVersion = Utils.parseJdkVersion()

        for (GuidesOption guidesOption : guidesOptionList) {
            BuildTool buildTool = guidesOption.buildTool
            TestFramework testFramework = guidesOption.testFramework
            Language lang = guidesOption.language

            if (!outputDir.exists()) {
                assert outputDir.mkdir()
            }

            for (App app: metadata.apps) {
                List<String> appFeatures = [] + app.features

                if (guidesOption.language == Language.GROOVY ||
                        !JDK_VERSIONS_SUPPORTED_BY_GRAALVM.contains(javaVersion)) {
                    appFeatures.remove('graalvm')
                }

                if (testFramework == TestFramework.SPOCK) {
                    appFeatures.remove('mockito')
                }

                // Normal guide use 'default' as name, multi project guides have different modules
                String appName = app.name == DEFAULT_APP_NAME ? StringUtils.EMPTY_STRING : app.name
                String folder = folderName(metadata.slug, guidesOption)

                Path destinationPath = Paths.get(outputDir.absolutePath, folder, appName)
                File destination = destinationPath.toFile()
                destination.mkdir()
                guidesGenerator.generateAppIntoDirectory(destination, app.applicationType, packageAndName, appFeatures, buildTool, testFramework, lang, javaVersion)

                final String srcFolder = 'src'
                Path srcPath = Paths.get(inputDir.absolutePath, appName, srcFolder)
                if (srcPath.toFile().exists()) {
                    Files.walkFileTree(srcPath, new CopyFileVisitor(Paths.get(destination.path, srcFolder)))
                }
                Path sourcePath = Paths.get(inputDir.absolutePath, appName, guidesOption.language.toString())
                if (!sourcePath.toFile().exists()) {
                    throw new GradleException("source folder " + sourcePath.toFile().absolutePath + " does not exist")
                }
                Files.walkFileTree(sourcePath, new CopyFileVisitor(destinationPath))
                if (app.excludeSource) {
                    for (String mainSource : app.excludeSource) {
                        deleteFile(destination, GuideAsciidocGenerator.mainPath(appName, mainSource), guidesOption)
                    }
                }
                if (app.excludeTest) {
                    for (String testSource : app.excludeTest) {
                        deleteFile(destination, GuideAsciidocGenerator.testPath(appName,  testSource, testFramework), guidesOption)
                    }
                }
                if (metadata.zipIncludes) {
                    File destinationRoot = new File(outputDir.absolutePath, folder)
                    for (String zipInclude : metadata.zipIncludes) {
                        copyFile(inputDir, destinationRoot, zipInclude)
                    }
                }
            }
        }
    }

    private void deleteFile(File destination, String path, GuidesOption guidesOption) {
        Paths.get(destination.absolutePath, path
                .replace("@lang@", guidesOption.language.toString())
                .replace("@languageextension@", guidesOption.language.extension))
                .toFile()
                .delete()
    }

    private void copyFile(File inputDir, File destinationRoot, String filePath) {
        File sourceFile = new File(inputDir, filePath)
        File destinationFile = new File(destinationRoot, filePath)

        File destinationFileDir = destinationFile.getParentFile()
        if (!destinationFileDir.exists()) {
            Files.createDirectories destinationFileDir.toPath()
        }

        Files.copy sourceFile.toPath(), destinationFile.toPath(), REPLACE_EXISTING
    }

    static List<GuidesOption> guidesOptions(GuideMetadata guideMetadata) {
        List<String> buildTools = guideMetadata.buildTools
        List<String> languages = guideMetadata.languages
        String testFramework = guideMetadata.testFramework
        List<GuidesOption> guidesOptionList = []

        if (buildTools.contains(BuildTool.GRADLE.toString())) {
            if (languages.contains(Language.JAVA.toString())) {
                guidesOptionList << createGuidesOption(BuildTool.GRADLE, Language.JAVA, testFramework)
            }
            if (languages.contains(Language.KOTLIN.toString())) {
                guidesOptionList << createGuidesOption(BuildTool.GRADLE, Language.KOTLIN, testFramework)
            }
            if (languages.contains(Language.GROOVY.toString())) {
                guidesOptionList << createGuidesOption(BuildTool.GRADLE, Language.GROOVY, testFramework)
            }
        }
        if (buildTools.contains(BuildTool.MAVEN.toString())) {
            if (languages.contains(Language.JAVA.toString())) {
                guidesOptionList << createGuidesOption(BuildTool.MAVEN, Language.JAVA, testFramework)
            }
            if (languages.contains(Language.KOTLIN.toString())) {
                guidesOptionList << createGuidesOption(BuildTool.MAVEN, Language.KOTLIN, testFramework)
            }
            if (languages.contains(Language.GROOVY.toString())) {
                guidesOptionList << createGuidesOption(BuildTool.MAVEN, Language.GROOVY, testFramework)
            }
        }
        guidesOptionList
    }

    private static GuidesOption createGuidesOption(@NonNull BuildTool buildTool,
                                                   @NonNull Language language,
                                                   @Nullable String testFramework) {
        new GuidesOption(buildTool, language, testFrameworkOption(language, testFramework))
    }

    private static TestFramework testFrameworkOption(@NonNull Language language,
                                                     @Nullable String testFramework) {
        if (testFramework != null) {
            return TestFramework.valueOf(testFramework.toUpperCase())
        }
        if (language == Language.GROOVY) {
            return TestFramework.SPOCK
        }
        TestFramework.JUNIT
    }
}
