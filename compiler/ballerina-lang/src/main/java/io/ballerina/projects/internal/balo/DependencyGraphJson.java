/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.projects.internal.balo;

import com.google.gson.annotations.SerializedName;
import io.ballerina.projects.model.Dependency;

import java.util.List;

/**
 * {@code DependencyGraphJson} Model for dependency graph JSON file.
 *
 * @since 2.0.0
 */
public class DependencyGraphJson {
    private static final String PACKAGE_DEPENDENCY_GRAPH = "package";
    @SerializedName(PACKAGE_DEPENDENCY_GRAPH) private List<Dependency> packageDependencyGraph;

    private static final String MODULE_DEPENDENCY_GRAPH = "module";
    @SerializedName(MODULE_DEPENDENCY_GRAPH) private List<ModuleDependency> moduleDependencyGraph;

    public DependencyGraphJson(List<Dependency> packageDependencyGraph, List<ModuleDependency> moduleDependencyGraph) {
        this.packageDependencyGraph = packageDependencyGraph;
        this.moduleDependencyGraph = moduleDependencyGraph;
    }

    public List<Dependency> getPackageDependencyGraph() {
        return packageDependencyGraph;
    }

    public void setPackageDependencyGraph(List<Dependency> packageDependencyGraph) {
        this.packageDependencyGraph = packageDependencyGraph;
    }

    public List<ModuleDependency> getModuleDependencyGraph() {
        return moduleDependencyGraph;
    }

    public void setModuleDependencyGraph(List<ModuleDependency> moduleDependencyGraph) {
        this.moduleDependencyGraph = moduleDependencyGraph;
    }
}
