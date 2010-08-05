/*
 * Copyright (c) 2005-2010 Grameen Foundation USA
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 *  See also http://www.apache.org/licenses/LICENSE-2.0.html for an
 *  explanation of the license and how it is applied.
 */

package org.mifos.platform.questionnaire.service;

import org.mifos.platform.util.CollectionUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ChoiceDetail implements Serializable {
    private static final long serialVersionUID = 5839636913158754732L;

    private String choiceText;
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value="SE_BAD_FIELD")
    private List<String> tags;

    public ChoiceDetail() {
        this(null);
    }

    public ChoiceDetail(String choiceText) {
        this.choiceText = choiceText;
        tags = new ArrayList<String>();
    }

    public String getChoiceText() {
        return choiceText;
    }

    public void setChoiceText(String choiceText) {
        this.choiceText = choiceText;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    public String getCommaSeparatedTags() {
        return CollectionUtils.toString(this.tags);
    }

    @Override
    public String toString() {
        return choiceText;
    }

    public void addTag(String tag) {
        tags.add(tag);
    }

    public void removeTag(int tagIndex) {
        tags.remove(tagIndex);
    }
}
