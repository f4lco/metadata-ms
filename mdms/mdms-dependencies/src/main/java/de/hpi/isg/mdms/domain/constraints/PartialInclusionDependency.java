/***********************************************************************************************************************
 * Copyright (C) 2014 by Sebastian Kruse
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 **********************************************************************************************************************/
package de.hpi.isg.mdms.domain.constraints;

import de.hpi.isg.mdms.model.common.AbstractHashCodeAndEquals;
import de.hpi.isg.mdms.model.constraints.Constraint;
import de.hpi.isg.mdms.model.util.ReferenceUtils;
import org.apache.commons.lang3.Validate;

import java.util.Arrays;

/**
 * {@link Constraint} implementation for partial n-ary inclusion dependencies.
 *
 * @author Sebastian Kruse
 */
public class PartialInclusionDependency extends AbstractHashCodeAndEquals implements Constraint {


    private final int[] dependentColumnIds, referencedColumnIds;

    private final double error, dependentSize;

    public PartialInclusionDependency(int dependentColumnId, int referencedColumnId, double error, double dependentSize) {
        this(new int[]{dependentColumnId}, new int[]{referencedColumnId}, error, dependentSize);
    }

    public PartialInclusionDependency(int[] dependentColumnIds, int[] referencedColumnIds, double error, double dependentSize) {
        Validate.isTrue(dependentColumnIds.length == referencedColumnIds.length);
        Validate.isTrue(ReferenceUtils.isSorted(dependentColumnIds));
        this.dependentColumnIds = dependentColumnIds;
        this.referencedColumnIds = referencedColumnIds;
        this.error = error;
        this.dependentSize = dependentSize;
    }

    public int[] getDependentColumnIds() {
        return this.dependentColumnIds;
    }

    public int[] getReferencedColumnIds() {
        return this.referencedColumnIds;
    }

    @Override
    public int[] getAllTargetIds() {
        int arity = this.getArity();
        int[] allIds = new int[arity * 2];
        System.arraycopy(this.dependentColumnIds, 0, allIds, 0, arity);
        System.arraycopy(this.referencedColumnIds, 0, allIds, arity, arity);
        return allIds;
    }

    @Override
    public String toString() {
        return String.format("%s \u2286 %s", Arrays.toString(this.dependentColumnIds), Arrays.toString(this.referencedColumnIds));
    }

    public int getArity() {
        return this.dependentColumnIds.length;
    }

    /**
     * Checks whether this instance is implied by another instance.
     *
     * @param that is the allegedly implying instance
     * @return whether this instance is implied
     */
    public boolean isImpliedBy(PartialInclusionDependency that) {
        if (this.getArity() > that.getArity()) {
            return false;
        }

        // Co-iterate the two INDs and make use of the sorting of the column IDs.
        int thisI = 0, thatI = 0;
        while (thisI < this.getArity() && thatI < that.getArity() && (this.getArity() - thisI <= that.getArity() - thatI)) {
            int thisCol = this.dependentColumnIds[thisI];
            int thatCol = that.dependentColumnIds[thatI];
            if (thisCol == thatCol) {
                thisCol = this.referencedColumnIds[thisI];
                thatCol = that.referencedColumnIds[thatI];
            }
            if (thisCol == thatCol) {
                thisI++;
                thatI++;
            } else if (thisCol > thatCol) {
                thatI++;
            } else {
                return false;
            }
        }

        return thisI == this.getArity();
    }

    /**
     * Tests this inclusion dependency for triviality, i.e., whether the dependent and referenced sides are equal.
     *
     * @return whether this is a trivial inclusion dependency
     */
    public boolean isTrivial() {
        return Arrays.equals(this.dependentColumnIds, this.referencedColumnIds);
    }

}