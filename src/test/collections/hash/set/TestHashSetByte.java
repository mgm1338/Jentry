package collections.hash.set;

import collections.hash.HashFunctions;
import core.Const;
import core.array.GrowthStrategy;
import core.array.factory.ArrayFactoryInt;
import core.array.factory.ArrayFactoryByte;
import core.stub.IntValueConverter;
import core.stub.*;
import junit.framework.TestCase;
import org.junit.Test;
import util.TestUtilsInt;
import util.TestUtilsByte;

/**
 * Copyright 1/14/13
 * All rights reserved.
 * <p/>
 * User: Max Miller
 * Created: 1/14/13
 */
public class TestHashSetByte
{
    HashSetByte hashSet;
    boolean template = ( this.getClass().getCanonicalName().contains( "_" ) );

    public static final int TEST_SIZE = 8;
    //when adding values around 0,1 can get results that would indicate correct behavior
    public static final int OFFSET_FROM_ZERO = 10;


    /**
     * Initially load with TEST_SIZE items, where the initial capacity is set to that size
     * Assert the sizes, and that entries are returned in compact manner
     */
    @Test
    public void loadTest()
    {
        if( template ) return;
        hashSet = new HashSetByte( TEST_SIZE );
        TestCase.assertTrue( hashSet.isEmpty() );
        TestCase.assertTrue( hashSet.getSize() == 0 );

        //initial fill up
        for( int i = 0; i < TEST_SIZE; i++ )
        {
            int j = hashSet.insert( IntValueConverter.byteFromInt( i + OFFSET_FROM_ZERO ) );
            TestCase.assertEquals( i, j ); //compact
            TestCase.assertTrue( hashSet.contains( IntValueConverter.byteFromInt( i + OFFSET_FROM_ZERO ) ) );
        }

        //fill up exact same will return the exact same entries
        for( int i = 0; i < TEST_SIZE; i++ )
        {
            int j = hashSet.insert( IntValueConverter.byteFromInt( i + OFFSET_FROM_ZERO ) );
            TestCase.assertEquals( i, j ); //compact
            TestCase.assertTrue( hashSet.contains( IntValueConverter.byteFromInt( i + OFFSET_FROM_ZERO ) ) );
        }


        TestCase.assertTrue( hashSet.getSize() == TEST_SIZE );
        TestCase.assertFalse( hashSet.isEmpty() );

        for( int i = 0; i < TEST_SIZE; i++ )
        {
            TestCase.assertEquals( hashSet.get( i ), IntValueConverter.byteFromInt( i + OFFSET_FROM_ZERO ) );
        }
    }

    /**
     * Load TEST_SIZE entries into the same bucket into the HashSet, this will show that collisions
     * do not effect insertion.
     */
    @Test
    public void sameBucketTest()
    {
        if( template ) return;

        //artificially making every item go into the same bucket
        hashSet = new HashSetByte( 8, 1.00, ArrayFactoryByte.defaultByteProvider,
                                            ArrayFactoryInt.defaultIntProvider, new SameBucketHashFunctionByte(),
                                            GrowthStrategy.doubleGrowth );

        TestCase.assertEquals( hashSet.getSize(), 0 );
        for( int i = 0; i < TEST_SIZE; i++ )
        {
            int j = hashSet.insert( IntValueConverter.byteFromInt( i + OFFSET_FROM_ZERO ) );
            TestCase.assertEquals( i, j ); //compact
            TestCase.assertTrue( hashSet.contains( IntValueConverter.byteFromInt( i + OFFSET_FROM_ZERO ) ) );

        }
        TestCase.assertEquals( hashSet.getSize(), TEST_SIZE );
        TestCase.assertEquals( TEST_SIZE, hashSet.bucketList.getSize() );
        TestCase.assertEquals( TEST_SIZE, hashSet.bucketList.getList( 0, null, false ).length );


    }

    @Test
    public void removeFromSameBucket()
    {
        if( template ) return;

        sameBucketTest();
        hashSet.remove( IntValueConverter.byteFromInt( 0 + OFFSET_FROM_ZERO ) ); //remove first
        TestCase.assertEquals( hashSet.size, TEST_SIZE - 1 );
        TestCase.assertTrue( hashSet.getEntry( IntValueConverter.byteFromInt( 0 + OFFSET_FROM_ZERO ) ) == Const.NO_ENTRY );
        TestCase.assertFalse( hashSet.contains( IntValueConverter.byteFromInt( 0 + OFFSET_FROM_ZERO ) ) );


        sameBucketTest(); //reprime
        hashSet.remove( IntValueConverter.byteFromInt( TEST_SIZE - 1 + OFFSET_FROM_ZERO ) ); //remove last
        TestCase.assertEquals( hashSet.size, TEST_SIZE - 1 );
        TestCase.assertTrue( hashSet.getEntry( IntValueConverter.byteFromInt( TEST_SIZE - 1 + OFFSET_FROM_ZERO ) )
                             == Const.NO_ENTRY );
        TestCase.assertFalse( hashSet.contains( IntValueConverter.byteFromInt( TEST_SIZE - 1 + OFFSET_FROM_ZERO ) ) );


        sameBucketTest(); //reprime
        hashSet.remove( IntValueConverter.byteFromInt( TEST_SIZE / 2 + OFFSET_FROM_ZERO ) ); //remove middle
        TestCase.assertEquals( hashSet.size, TEST_SIZE - 1 );
        TestCase.assertTrue( hashSet.getEntry( IntValueConverter.byteFromInt( TEST_SIZE / 2 + OFFSET_FROM_ZERO ) )
                             == Const.NO_ENTRY );
        TestCase.assertFalse( hashSet.contains( IntValueConverter.byteFromInt( TEST_SIZE / 2 + OFFSET_FROM_ZERO ) ) );


        sameBucketTest();
        //remove all three
        hashSet.remove( IntValueConverter.byteFromInt( 0 + OFFSET_FROM_ZERO ) ); //remove first
        TestCase.assertEquals( hashSet.size, TEST_SIZE - 1 );
        TestCase.assertTrue( hashSet.getEntry( IntValueConverter.byteFromInt( 0 + OFFSET_FROM_ZERO ) ) == Const.NO_ENTRY );
        hashSet.remove( IntValueConverter.byteFromInt( TEST_SIZE - 1 + OFFSET_FROM_ZERO ) ); //remove last
        TestCase.assertEquals( hashSet.size, TEST_SIZE - 2 );
        TestCase.assertTrue( hashSet.getEntry( IntValueConverter.byteFromInt( TEST_SIZE - 1 + OFFSET_FROM_ZERO ) )
                             == Const.NO_ENTRY );
        hashSet.remove( IntValueConverter.byteFromInt( TEST_SIZE / 2+OFFSET_FROM_ZERO ) ); //remove middle
        TestCase.assertEquals( hashSet.size, TEST_SIZE - 3 );
        TestCase.assertTrue( hashSet.getEntry( IntValueConverter.byteFromInt( TEST_SIZE / 2 + OFFSET_FROM_ZERO ) )
                             == Const.NO_ENTRY );

        TestCase.assertFalse( hashSet.contains( IntValueConverter.byteFromInt( TEST_SIZE - 1 + OFFSET_FROM_ZERO ) ) );
        TestCase.assertFalse( hashSet.contains( IntValueConverter.byteFromInt( TEST_SIZE / 2 + OFFSET_FROM_ZERO ) ) );
        TestCase.assertFalse( hashSet.contains( IntValueConverter.byteFromInt( 0 + OFFSET_FROM_ZERO ) ) );


    }

    /** Remove each item iteratively */
    @Test
    public void fullRemove()
    {
        if( template ) return;

        loadTest();//fill up

        //remove each item iteratively, asserting it was removed
        for( int i = 0; i < TEST_SIZE; i++ )
        {
            TestCase.assertTrue( hashSet.remove( IntValueConverter.byteFromInt( i + OFFSET_FROM_ZERO ) ) );
            TestCase.assertEquals( hashSet.getSize(), ( TEST_SIZE - i - 1 ) );
            TestCase.assertTrue( hashSet.getEntry( IntValueConverter.byteFromInt( i + OFFSET_FROM_ZERO ) )
                                 == Const.NO_ENTRY );
        }
        TestCase.assertTrue( hashSet.getSize() == 0 );
        TestCase.assertTrue( hashSet.isEmpty() );


    }

    /** Load more items into the HashSet than its initial capacity can accommodate */
    @Test
    public void growthTest()
    {
        if( template ) return;
        hashSet = new HashSetByte( TEST_SIZE );

        for( int i = 0; i < TEST_SIZE * 4; i++ )
        {
            int j = hashSet.insert( IntValueConverter.byteFromInt( i + OFFSET_FROM_ZERO ) );
            TestCase.assertEquals( i, j ); //compact
        }

        TestCase.assertTrue( hashSet.getSize() == TEST_SIZE * 4 );
        TestCase.assertFalse( hashSet.isEmpty() );

    }

    //When an item isnt in HashSet, should return false
    @Test
    public void assertRemoveIsNotThereFalse()
    {
        if( template ) return;
        fullRemove();
        TestCase.assertFalse( hashSet.remove( IntValueConverter.byteFromInt( 1000 ) ) );

    }

    @Test
    public void freeListCompactNessTest()
    {
        if( template ) return;
        loadTest();
        hashSet.remove( IntValueConverter.byteFromInt( 0 + OFFSET_FROM_ZERO ) ); //remove first
        //should take first spot
        TestCase.assertTrue( hashSet.insert( IntValueConverter.byteFromInt( 1000 ) ) == 0 );
        TestCase.assertTrue( hashSet.contains( IntValueConverter.byteFromInt( 1000 ) ) );
        TestCase.assertFalse( hashSet.contains( IntValueConverter.byteFromInt( 0 ) ) );

    }

    @Test
    public void growFreeListTest()
    {
        if( template ) return;
        growthTest();

        //doing a remove of all the items
        for( int i = 0; i < TEST_SIZE * 4; i++ )
        {
            TestCase.assertTrue( hashSet.remove( IntValueConverter.byteFromInt( i + OFFSET_FROM_ZERO ) ) );
        }
        TestCase.assertTrue( hashSet.isEmpty() );
        TestCase.assertTrue( hashSet.freeList.length >= TEST_SIZE * 4 );
        //insert all
        for( int i = 0; i < TEST_SIZE * 4; i++ )
        {
            int j = hashSet.insert( IntValueConverter.byteFromInt( i + OFFSET_FROM_ZERO ) );
            TestCase.assertTrue( j < ( TEST_SIZE * 4 ) ); //compact (although with massive removes will replace
            //last item removed first).
        }

        TestCase.assertTrue( hashSet.getSize() == TEST_SIZE * 4 );
        TestCase.assertFalse( hashSet.isEmpty() );


    }

    @Test
    public void clearTest()
    {
        if( template ) return;
        hashSet = new HashSetByte( TEST_SIZE );
        //fill
        for( int i = 0; i < TEST_SIZE; i++ )
        {
            int j = hashSet.insert( IntValueConverter.byteFromInt( i + OFFSET_FROM_ZERO ) );
            TestCase.assertEquals( i, j ); //compact
            TestCase.assertTrue( hashSet.contains( IntValueConverter.byteFromInt( i + OFFSET_FROM_ZERO ) ) );
        }


        TestCase.assertTrue( hashSet.getSize() == TEST_SIZE );
        TestCase.assertFalse( hashSet.isEmpty() );

        for( int i = 0; i < TEST_SIZE; i++ )
        {
            TestCase.assertEquals( hashSet.get( i ), IntValueConverter.byteFromInt( i + OFFSET_FROM_ZERO ) );
        }
        //clear
        hashSet.clear();
        //assert size and empty

        TestCase.assertTrue( hashSet.getSize() == 0 );
        TestCase.assertTrue( hashSet.isEmpty() );

        // fill up again more stuff
        for( int i = 0; i < TEST_SIZE * 4; i++ )
        {
            int j = hashSet.insert( IntValueConverter.byteFromInt( i + OFFSET_FROM_ZERO ) );
            TestCase.assertEquals( i, j ); //compact
            TestCase.assertTrue( hashSet.contains( IntValueConverter.byteFromInt( i + OFFSET_FROM_ZERO ) ) );
        }

        TestCase.assertTrue( hashSet.getSize() == TEST_SIZE * 4 );
        TestCase.assertFalse( hashSet.isEmpty() );

        for( int i = 0; i < TEST_SIZE * 4; i++ )
        {
            TestCase.assertEquals( hashSet.get( i ), IntValueConverter.byteFromInt( i + OFFSET_FROM_ZERO ) );
        }
        hashSet.clear();

        TestCase.assertTrue( hashSet.getSize() == 0 );
        TestCase.assertTrue( hashSet.isEmpty() );

    }


    //free list, exactly same with same pointer
    //keys, same keys
    //size is same, rehash size is same, load factor same
    //same growth, factories
    @Test
    public void fullCopyValidSetTest()
    {
        if( template ) return;
        loadTest();
        HashSetByte copy = hashSet.copy( null );
        assertEquals( hashSet, copy );
    }

    /**
     * Copy to a larger HashSet
     */
    @Test
    public void copyToLargerSet()
    {
        if( template ) return;
        loadTest();
        HashSetByte copy = hashSet.copy( new HashSetByte( 4096 ) );
        assertEquals( hashSet, copy );
    }


    /**
     * Copy to a smaller set
     */
    @Test
    public void copyFromSmaller()
    {
        if( template ) return;
        loadTest();
        HashSetByte copy = hashSet.copy( new HashSetByte( 1 ) );
        assertEquals( hashSet, copy );

    }

    /**
     * Test the equality of all state of the HashSet. If we are copying to a much larger set,
     * we will assert up to the expected results. If copying from smaller, we expect it to grow.
     *
     * @param expected expected results
     * @param actual what we actually have
     */
    protected void assertEquals(HashSetByte expected, HashSetByte actual)
    {
        TestUtilsByte.assertArrayContentsToLen( expected.keys, actual.keys, expected.keys.length );
        TestUtilsInt.assertArrayContentsEqual( expected.freeList, actual.freeList );
        TestCase.assertEquals( expected.loadFactor, actual.loadFactor );
        TestCase.assertEquals( expected.loadFactorSize, actual.loadFactorSize);
        TestCase.assertEquals( expected.getSize(), actual.getSize());
        TestCase.assertEquals( expected.getNextEntry(), actual.getNextEntry());

    }

    protected class SameBucketHashFunctionByte extends HashFunctions.HashFunctionByte
    {
        @Override
        public int getHashCode( byte k )
        {
            return 0;
        }
    }

}
