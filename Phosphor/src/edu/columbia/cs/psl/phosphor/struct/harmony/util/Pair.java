
/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  From Apache Harmony (http://harmony.apache.org/)
 *  Modification: package changed to one that is ignored (i.e., not instrumented) by Phosphor, change class visibility
 *  to public
 */
package edu.columbia.cs.psl.phosphor.struct.harmony.util;

/**
 * This class implements a generic pair.
 */
public final class Pair<E, T> {
    private E comp1;

    private T comp2;

    /**
     * @param comp1
     *            first component
     * @param comp2
     *            second component
     */
    public Pair(E comp1, T comp2) {
        this.comp1 = comp1;
        this.comp2 = comp2;
    }

    /**
     * @return the first component of pair
     */
    public final E getFirst() {
        return comp1;
    }

    /**
     * @return the second component of pair
     */
    public final T getSecond() {
        return comp2;
    }

    /**
     * @param o
     *            the object that will be compared for equality.
     * @return <code>true</code> if <code>obj</code> is instance of the
     *         class <code>Pair</code>, else return <code>false</code>
     */
    public final boolean equals(Object o) {

        if (o == null)
            return false;
        if (o == this)
            return true;
        if (!(o instanceof Pair))
            return false;
        Pair oPair = (Pair) o;
        return (this.comp1.equals(oPair.comp1) && this.comp2
                .equals(oPair.comp2));
    }

    /**
     * @return the hash code for this object.
     */
    public final int hashCode() {
        return this.comp1.hashCode() ^ this.comp2.hashCode();
    }

    /**
     * @return a description of the contained of an object of this class
     */
    public final String toString() {
        return "PAIR = first: " + comp1 + " | " + " second: " + comp2;
    }
}
