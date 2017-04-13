/*
 * Copyright [2017] [Andrea Romagnoli]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.elasticsearch.plugin.ingest.xml;

// Keeps information about the name and occurrences of each field
class Field {

    String name;
    int count;

    public Field( String name ) {
        this.name = name;
        this.count = 1;
    }

    public Field( String name, int count ) {
        this.name = name;
        this.count = count;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public void setCount( int count ) {
        this.count = count;
    }

    public String getName() {
        return this.name;
    }

    public int getCount() {
        return this.count;
    }

    public int increase() {
        this.count = this.count + 1;
        return this.count;
    }
}

