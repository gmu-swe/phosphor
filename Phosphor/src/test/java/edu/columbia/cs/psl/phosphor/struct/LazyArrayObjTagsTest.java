package edu.columbia.cs.psl.phosphor.struct;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class LazyArrayObjTagsTest {

    /** Equals method utilizes underlying array value **/
    @Test
    public void deepEqualIfUnderlyingValIsDeepEqual() {

        long[] longs = {1, 3, 3, 7};
        LazyArrayObjTags lazyArrayObjTags1 = new LazyLongArrayObjTags(longs);
        LazyArrayObjTags lazyArrayObjTags2 = new LazyLongArrayObjTags(longs);

        Assert.assertEquals(lazyArrayObjTags1, lazyArrayObjTags2);
    }
}