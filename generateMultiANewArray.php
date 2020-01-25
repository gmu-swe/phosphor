<?php
$template = '
    public static LazyReferenceArrayObjTags MULTIANEWARRAY_DESC_2DIMS(int dim1, Taint t1, int dim2, Taint t2) {
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyByteArrayObjTags[dim1]);
        for (int i = 0; i < dim1; i++) {
            ret.val[i] = new LazyByteArrayObjTags(t2, new byte[dim2]);
        }
        return ret;
    }
    public static LazyReferenceArrayObjTags MULTIANEWARRAY_DESC_2DIMS(int dim, Taint tag) {
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(tag, new LazyByteArrayObjTags[dim]);
        return ret;
    }
    
    public static LazyReferenceArrayObjTags MULTIANEWARRAY_DESC_3DIMS(int dim1, Taint t1, int dim2, Taint t2) {
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyReferenceArrayObjTags[dim1]);
        for (int i = 0; i < dim1; i++) {
            ret.val[i] = new LazyReferenceArrayObjTags(t2, new LazyByteArrayObjTags[dim2]);
        }
        return ret;
    }
    
      public static LazyReferenceArrayObjTags MULTIANEWARRAY_DESC_3DIMS(int dim1, Taint t1, int dim2, Taint t2, int dim3, Taint t3) {
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyReferenceArrayObjTags[dim1]);
        for (int i = 0; i < dim1; i++) {
            LazyReferenceArrayObjTags d = new LazyReferenceArrayObjTags(t2, new LazyByteArrayObjTags[dim2]);
            ret.val[i] = d;
            for (int j = 0; j < dim2; j++) {
                d.val[j] = new LazyByteArrayObjTags(t3, new byte[dim3]);
            }
        }
        return ret;
    }
    
        public static LazyReferenceArrayObjTags MULTIANEWARRAY_DESC_4DIMS(int dim1, Taint t1, int dim2, Taint t2, int dim3, Taint t3) {
        LazyReferenceArrayObjTags ret = new LazyReferenceArrayObjTags(t1, new LazyReferenceArrayObjTags[dim1]);
        for (int i = 0; i < dim1; i++) {
            LazyReferenceArrayObjTags d = new LazyReferenceArrayObjTags(t2, new LazyReferenceArrayObjTags[dim2]);
            ret.val[i] = d;
            for (int j = 0; j < dim2; j++) {
                d.val[j] = new LazyReferenceArrayObjTags(t3, new LazyByteArrayObjTags[dim3]);
            }
        }
        return ret;
    }
';
$types = ["Byte", "Boolean", "Char", "Float", "Int", "Double", "Short", "Long"];
foreach ($types as $typ) {
    $char = substr($typ, 0,1);
    if ($char == "L")
        $char = "J";
    if ($typ == "Boolean")
        $char = "Z";
    print str_replace("Byte", $typ, str_replace("byte", strtolower($typ), str_replace("DESC",$char,$template)));
}
?>
