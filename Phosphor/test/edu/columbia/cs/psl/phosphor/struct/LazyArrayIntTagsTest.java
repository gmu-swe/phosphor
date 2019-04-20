package edu.columbia.cs.psl.phosphor.struct;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class LazyArrayIntTagsTest {

    /** Equals method utilizes underlying array value **/
    @Test
    public void deepEqualIfUnderlyingValIsDeepEqual() {

        long[] longs = {1, 3, 3, 7};
        LazyArrayIntTags lazyArrayObjTags1 = new LazyLongArrayIntTags(longs);
        LazyArrayIntTags lazyArrayObjTags2 = new LazyLongArrayIntTags(longs);

        Assert.assertEquals(lazyArrayObjTags1, lazyArrayObjTags2);
    }
}