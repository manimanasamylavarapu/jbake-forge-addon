/**
 * Copyright 2014 JBake
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jbake.forge.addon.facets.impl;

import org.jbake.forge.addon.facets.AbstractJBakeFacet;
import org.jbake.forge.addon.types.BuildSystemType;
import org.jbake.forge.addon.types.ContentType;
import org.jbake.forge.addon.types.PublishType;
import org.jbake.forge.addon.types.TemplateType;
import org.jbake.forge.addon.utils.ContentUtil;
import org.jbake.forge.addon.utils.TemplateUtil;
import org.jboss.forge.addon.dependencies.Coordinate;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.builder.CoordinateBuilder;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.maven.plugins.ExecutionBuilder;
import org.jboss.forge.addon.maven.plugins.MavenPlugin;
import org.jboss.forge.addon.maven.plugins.MavenPluginAdapter;
import org.jboss.forge.addon.maven.plugins.MavenPluginBuilder;
import org.jboss.forge.addon.maven.projects.MavenBuildSystem;
import org.jboss.forge.addon.maven.projects.MavenPluginFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.dependencies.DependencyInstaller;
import org.jboss.forge.addon.projects.facets.DependencyFacet;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.furnace.versions.SingleVersion;
import org.jboss.forge.furnace.versions.Version;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Rajmahendra Hegde <rajmahendra@gmail.com>
 *         modified by @author Mani Manasa Mylavarapu <manimanasamylavarapu@gmail.com>
 */
public class JBakeFacetImpl_2_0 extends AbstractJBakeFacet {
    protected final DependencyInstaller installer;

    public BuildSystemType buildSystemType;
    public TemplateType templateType;
    String jbakeFolderPath;
    ContentType contentType;
    PublishType pageStatusType;
    String postTitle;
    String pageTitle;
    String targetDirectory;
    String creationOrModificationDate;
    String tags;
    PublishType postStatusType;

    @Inject
    public JBakeFacetImpl_2_0(final DependencyInstaller installer) {
        this.installer = installer;
    }

    public static final Dependency KUALI_MAVEN_DEPENDENCY =
            DependencyBuilder
                    .create("org.kuali.maven.plugins:wagon-maven-plugin").setVersion("1.0.3");
    public static final Dependency JBAKE_CORE_DEPENDENCY =
            DependencyBuilder
                    .create("org.jbake:jbake-core").setVersion("2.3.2").
                    addExclusion(CoordinateBuilder.create().setGroupId("org.eclipse.jetty").setArtifactId("jetty-server"));
    public static final Dependency JBAKE_MAVEN_POM_DEPENDENCY =
            DependencyBuilder
                    .create("br.com.ingenieux:jbake-maven-plugin")
                    .setPackaging("pom")
                    .setVersion("0.0.9");
    @Inject
    private ProjectFactory projectFactory;

    public String getJbakeFolderPath() {
        return jbakeFolderPath;
    }

    public void setJbakeFolderPath(String jbakeFolderPath) {
        this.jbakeFolderPath = jbakeFolderPath;
    }

    @Override
    public void setTemplateType(TemplateType templateType) {
        this.templateType = templateType;
    }

    @Override
    public void installJbakeCoreDependencies() {
        this.installer.install(getFaceted(), JBAKE_CORE_DEPENDENCY);
    }

    @Override
    public boolean isJbakeInstalled() {
        if (isJbakeFolderCreated() && isDependencyRequirementsMet())
            return true;
        else
            return false;

    }

    @Override
    public boolean isDependencyRequirementsMet() {
        boolean isInstalled = false;
        DependencyFacet dependencyFacet = origin
                .getFacet(DependencyFacet.class);
        // create an iterator
        Set<Dependency> requiredDependencies = getRequiredDependencyOptions();
        Iterator iterator = requiredDependencies.iterator();
        while (iterator.hasNext()) {
            Dependency dependency = (Dependency) iterator.next();
            if (dependencyFacet.hasEffectiveDependency(dependency)) {
                isInstalled = true;

            }
        }
        return isInstalled;
    }

    @Override
    public void createJbakeFolderStructure() throws IOException {
        TemplateUtil.unzip(getTemplateType().toString(), jbakeFolderPath);

    }

    @Override
    public void installMavenPluginDependencies() {
        Coordinate warCompiler = CoordinateBuilder.create("org.apache.maven.plugins:maven-war-plugin").setVersion("2.4");
        Coordinate jbakeCompiler = CoordinateBuilder.create("br.com.ingenieux:jbake-maven-plugin");

        MavenPluginBuilder warBuilder = MavenPluginBuilder.create().setCoordinate(warCompiler);
        MavenPluginBuilder jbakeBuilder = MavenPluginBuilder.create()
                .setCoordinate(jbakeCompiler).addExecution(ExecutionBuilder.create().setId("default-generate").setPhase("generate-resources").addGoal("generate"));

        MavenPlugin warPlugin = new MavenPluginAdapter(warBuilder);
        MavenPlugin jbakePlugin = new MavenPluginAdapter(jbakeBuilder);

        MavenPluginFacet pluginFacet = getFaceted().getFacet(MavenPluginFacet.class);

        pluginFacet.addPlugin(warPlugin);
        pluginFacet.addPlugin(jbakePlugin);

    }

    @Override
    public boolean install() {
        if (isJbakeInstalled()) {
            return false;
        } else {
            try {
                setAbsoluteJbakeFolderPath(buildSystemType);
                createJbakeFolderStructure();
            } catch (IOException e) {
                e.printStackTrace();
            }
            installJbakeCoreDependencies();
            if (buildSystemType == (BuildSystemType.maven)) {
                installMavenPluginDependencies();
            } else {

            }

            return true;
        }

    }

    private void setAbsoluteJbakeFolderPath(BuildSystemType buildType) throws IOException {
        Project selectedProject = getFaceted();
        DirectoryResource directoryResource = (DirectoryResource) selectedProject.getRoot();
        File codeFolder = directoryResource.getUnderlyingResourceObject();
        String outputFilePath = null;
        if (buildType == (BuildSystemType.maven)) {
            jbakeFolderPath = codeFolder.getCanonicalPath() + "/src/main/jbake";

        } else {
            jbakeFolderPath = codeFolder.getCanonicalPath() + "/src/jbake";
        }

    }

    @Override
    public boolean isInstalled() {
        return false;
    }

    @Inject
    private MavenBuildSystem buildSystem;

    @Override
    public BuildSystemType getBuildSystemType() {
        return buildSystemType;
    }

    @Override
    public void setBuildSystemType(BuildSystemType buildSystemType) {
        this.buildSystemType = buildSystemType;
    }

    @Override
    public Set<Dependency> getRequiredDependencyOptions() {
        Set<Dependency> dependencies = new HashSet<Dependency>();
        dependencies.add(JBAKE_CORE_DEPENDENCY);
        if (buildSystemType == BuildSystemType.maven) {
            // dependencies.add(KUALI_MAVEN_DEPENDENCY);
            dependencies.add(JBAKE_MAVEN_POM_DEPENDENCY);
        }

        return dependencies;
    }

    @Override
    public void setContentType(ContentType contentType) {
        this.contentType = contentType;
    }

    @Override
    public void setPageStatusType(PublishType pageStatusType) {
        this.pageStatusType = pageStatusType;
    }

    @Override
    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
    }

    @Override
    public void setTargetDirectory(String targetDirectory) {
        this.targetDirectory = targetDirectory;
    }

    @Override
    public void setCreationOrModificationDate(String creationOrModificationDate) {
        this.creationOrModificationDate = creationOrModificationDate;
    }

    @Override
    public void setPageTags(String tags) {
        this.tags = tags;
    }

    @Override
    public void setPostTitle(String postTitle) {
        this.postTitle = postTitle;
    }

    @Override
    public void setPostTags(String tags) {
        this.tags = tags;
    }

    @Override
    public boolean createPage() throws IOException {
        Boolean isCreated = false;
        if (contentType == (ContentType.AsciiDoc)) {
            ContentUtil.createFile(targetDirectory, contentType, pageTitle);
            isCreated = ContentUtil.writeFile(contentType, pageTitle, creationOrModificationDate, "page", tags, pageStatusType);
        } else if (contentType == (ContentType.HTML)) {
            ContentUtil.createFile(targetDirectory, contentType, pageTitle);
            isCreated = ContentUtil.writeFile(contentType, pageTitle, creationOrModificationDate, "page", tags, pageStatusType);
        } else if (contentType == (ContentType.Markdown)) {
            ContentUtil.createFile(targetDirectory, contentType, pageTitle);
            isCreated = ContentUtil.writeFile(contentType, pageTitle, creationOrModificationDate, "page", tags, pageStatusType);
        }
        return isCreated;
    }

    @Override
    public boolean createPost() throws IOException {
        Boolean isCreated = false;
        if (contentType == (ContentType.AsciiDoc)) {
            ContentUtil.createFile(targetDirectory, contentType, postTitle);
            isCreated = ContentUtil.writeFile(contentType, postTitle, creationOrModificationDate, "post", tags, postStatusType);
        } else if (contentType == (ContentType.HTML)) {
            ContentUtil.createFile(targetDirectory, contentType, postTitle);
            isCreated = ContentUtil.writeFile(contentType, postTitle, creationOrModificationDate, "post", tags, postStatusType);
        } else if (contentType == (ContentType.Markdown)) {
            ContentUtil.createFile(targetDirectory, contentType, postTitle);
            isCreated = ContentUtil.writeFile(contentType, postTitle, creationOrModificationDate, "post", tags, postStatusType);
        }
        return isCreated;
    }

    @Override
    public void setPostStatusType(PublishType postStatusType) {
        this.postStatusType = postStatusType;
    }

    @Override
    public TemplateType getTemplateType() {
        return templateType;
    }

    @Override
    public boolean isJbakeFolderCreated() {
        Project selectedProject = getFaceted();
        DirectoryResource directoryResource = (DirectoryResource) selectedProject.getRoot();
        File codeFolder = directoryResource.getUnderlyingResourceObject();
        String filePathStringForMaven = null;
        String filePathStringForGradle = null;
        /*if (buildSystemType == (BuildSystemType.maven)) {
            try {
                filePathString = codeFolder.getCanonicalPath() + "/src/main/jbake";
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                filePathString = codeFolder.getCanonicalPath() + "/src/jbake";
            } catch (IOException e) {
                e.printStackTrace();
            }
        }*/

        try {
            filePathStringForMaven = codeFolder.getCanonicalPath() + "/src/main/jbake";
            filePathStringForGradle = codeFolder.getCanonicalPath() + "/src/jbake";
        } catch (IOException e) {
            e.printStackTrace();
        }

        File f1 = new File(filePathStringForMaven);
        File f2 = new File(filePathStringForGradle);
        if (f1.exists() || f2.exists()) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Version getSpecVersion() {
        return new SingleVersion("2.0");
    }

}