/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.model.internal.core;

import org.gradle.api.Action;
import org.gradle.model.internal.core.rule.describe.ModelRuleDescriptor;

import java.util.Collections;
import java.util.List;

public class ActionBackedMutateRule<T> implements ModelAction<T> {
    private final ModelReference<T> subject;
    private final Action<? super T> configAction;
    private final ModelRuleDescriptor descriptor;

    public ActionBackedMutateRule(ModelReference<T> subject, Action<? super T> configAction, ModelRuleDescriptor descriptor) {
        this.subject = subject;
        this.configAction = configAction;
        this.descriptor = descriptor;
    }

    @Override
    public ModelReference<T> getSubject() {
        return subject;
    }

    @Override
    public void execute(MutableModelNode modelNode, T object, Inputs inputs) {
        configAction.execute(object);
    }

    @Override
    public List<ModelReference<?>> getInputs() {
        return Collections.emptyList();
    }

    @Override
    public ModelRuleDescriptor getDescriptor() {
        return descriptor;
    }
}
