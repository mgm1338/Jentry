package collections.relationship;


import collections.Collection;
import collections.hash.set.HashSetLong;
import core.Const;
import core.NumberUtil;
import core.array.GrowthStrategy;
import core.array.factory.ArrayFactoryInt;

import java.util.Arrays;

/**
 * Copyright 1/27/13
 * All rights reserved.
 * <p/>
 * User: Max Miller
 * Created: 1/27/13
 * <p/>
 * <p/>
 * <p>IMPORTANT! THIS STRUCTURE ASSUME THAT THE LEFTS INSERTED ARE (Mostly) COMPACT. IF THEY ARE NOT, INSERT
 * INTO A JENTRY COLLECTION AND INSERT THE HANDLES.</p>
 * <p>
 * A collection of One (left) to Many (right) ints. This is used in conjunction
 * with the Jentry collections, associating the int handles together. The One to Many allows
 * for iteration over the associations from a left, going in order, as well as checking to see if a left
 * and right are associated.
 * </p>
 * <p/>
 * <p>The actual associations are held in a HashSetLong, using {@link core.NumberUtil#packLong(int, int)}, where
 * the right and left compose a long, which helps ensure that the same association is not added twice. The entries
 * in the HashSet become the key components to our collection. </p>
 * <p/>
 * <p>The internal structure of the OneToManyInt will take in two ints, compose them into a Long. The entries
 * into this HashSet will be stored in a unique way. The first inserted entry will take the index in lefts for
 * the actual left value (for instance, if we are associating (0,2), then index 0 will hold the HashSet entry
 * for the long composed of 0 and 2 (2). This fact is why the important note of usage, that we do not want to
 * associate only lefts that are sparse, for it will quickly allocate the lefts array.</p>
 * <p/>
 * <p>
 * Simple diagram inserting {1,3}, {1,4},{1,5},{0,1}. Followed by removal of {1,4}. Lastly re-use this index by
 * inserting {0,6}.
 * <p/>
 * Inserting {1-3}, gets handle of 0 from <b>associations</b>.
 * Insert 0 into lefts[1].
 * <pre>
 *      lefts   leftNexts
 * 0
 * 1     0
 * 2
 * </pre>
 * Inserting {1-4}, gets handle of 1 from <b>associations</b>
 * Follow the index of lefts[1], insert 1 into leftNexts[0]
 * <pre>
 *      lefts   leftNexts
 * 0               1
 * 1     0
 * 2
 * </pre>
 * Inserting {1-5}, gets handle of 2 from <b>associations</b>.We follow the list, and insert leftNexts into
 * index 2.
 * <pre>
 *      lefts   leftNexts
 * 0               1
 * 1     0         2
 * 2
 * </pre>
 * Inserting {0-1}, gets handle of 3 from <b>associations</b>. This is another simple entry of 3 into lefts[0].
 * <pre>
 *      lefts   leftNexts
 * 0     3         1
 * 1     0         2
 * 2
 * </pre>
 * Now, finally we have a removal of the second item of left of 1. In this case, we 'bubble' up the 2 to leftNexts[0].
 * <pre>
 *      lefts   leftNexts
 * 0     3         2
 * 1     0
 * 2
 * </pre>
 * <p/>
 * Note that an insertion now will result in index 1. We can continue to insert as normal,
 * and it will re-use the index.  We insert {0,6}, which gets handle 1.
 * <pre>
 *      lefts   leftNexts
 * 0     3         2
 * 1     0
 * 2
 * 3               1
 * </pre>
 * </p>
 */
public class OneToManyInt implements Collection
{
    /** Growth strategy for growing the lefts, nexts, and counts(if tracking) */
    protected final GrowthStrategy growthStrategy;
    /** Factory used to allocate and grow the int arrays */
    protected final ArrayFactoryInt intFactory;
    private static final int DEFAULT_REL_SIZE = 2;

    /** HashSet of longs that will hold the associations of the OneToMany */
    protected HashSetLong associations;
    /** Array that will hold the first relationship from the index, will be the handle into <b>associations</b> */
    protected int[] lefts;
    /** The next pointer that will fill the index of the current entry to the next handle (see class docs) */
    protected int[] leftNexts;
    /** An optional parallel array that will track the count of the associations for each left */
    protected int[] leftCounts = null;
    /** By default, we do not use this array */
    protected final boolean countLefts;
    /** Associations size tracker */
    protected int size = 0;

    /**
     * Constructor
     *
     * @param initialLefts        the initial number of lefts that we can hold (note that is it compact, so inserting
     *                            above this number will immediately cause a growth)
     * @param initialAssociations the size of initial number of associations we can make.
     * @param countLefts          whether or not to track the counts of our associations per left.
     */
    public OneToManyInt( int initialLefts, int initialAssociations, boolean countLefts )
    {
        this( initialLefts, initialAssociations, countLefts, GrowthStrategy.doubleGrowth,
              ArrayFactoryInt.defaultIntProvider );
    }

    /**
     * Full Constructor
     *
     * @param initialLefts        the initial number of lefts that we can hold (note that is it compact, so inserting
     *                            above this number will immediately cause a growth)
     * @param initialAssociations the size of initial number of associations we can make.
     * @param countLefts          whether or not to track the counts of our associations per left.
     * @param growthStrategy      the growth strategy for growing our lefts array, associations array,
     *                            and counts array (if counting the associations)
     * @param intFactory          factory used to grow/allocate the arrays
     */
    public OneToManyInt( int initialLefts, int initialAssociations, boolean countLefts,
                         GrowthStrategy growthStrategy, ArrayFactoryInt intFactory )
    {
        this.growthStrategy = growthStrategy;
        this.intFactory = intFactory;
        this.countLefts = countLefts;
        if( countLefts )
        {
            leftCounts = intFactory.alloc( initialLefts );
        }
        lefts = intFactory.alloc( initialLefts, Const.NO_ENTRY );
        leftNexts = intFactory.alloc( initialAssociations, Const.NO_ENTRY );
        associations = new HashSetLong( initialAssociations );

    }

    /**
     * <p>
     * Associate two integers, insert the composed long into our associations. Insert the resulting handle into our
     * singly linked list type of structure. See {@link OneToManyInt} base description for a more verbose description
     * of the insertion process via example.
     * </p>
     * <p>
     * This associates one 'left' to 0-n 'rights', so we can iterate over the associations from one left. If we need
     * the ability to do iterations from both the left and right, use the //TODO{@link ManyToManyInt} structure.
     * We return a handle to this association, so parallel information related to this association can be stored in
     * a compact array (similar to what we do internally in the structure).
     * </p>
     *
     * @param left  left integer, this is the "one" side
     * @param right right integer, this is one of the 'many' rights associated with the specified left
     * @return the handle to this association
     */
    public int associate( int left, int right )
    {
        long composed = NumberUtil.packLong( left, right );
        int preSize = associations.getSize();
        int handle = associations.insert( composed );
        if (associations.getSize()==preSize) //this checks to see if we inserted without re-hashing, if we did not
        //insert, simple return the handle
        {
            return handle;
        }
        lefts = intFactory.ensureArrayCapacity( lefts, left + 1, Const.NO_ENTRY, growthStrategy );
        if( countLefts )
        {
            leftCounts = intFactory.ensureArrayCapacity( leftCounts, left + 1, growthStrategy );
            leftCounts[ left ]++;
        }
        //we will never grow the leftNexts past the handle +1 (we use the handle to determine where we are
        //inserting into the array)
        leftNexts = intFactory.ensureArrayCapacity( leftNexts, handle + 1, Const.NO_ENTRY, growthStrategy );

        int testInsert = lefts[ left ];
        if( testInsert == Const.NO_ENTRY ) //first item, insert into the left array
        {
            lefts[ left ] = handle;
        }
        else
        {
            int prev = testInsert;
            testInsert = leftNexts[ testInsert ]; //cycle through leftNexts
            if (testInsert==Const.NO_ENTRY)
            {
                leftNexts[ prev ] = handle;

            }
            else
            {
                while (testInsert!=Const.NO_ENTRY)
                {
                    prev = testInsert;
                    testInsert = leftNexts[testInsert];
                }
                leftNexts[prev] = handle;
            }
        }
        size++;
        return handle;
    }

    /**
     * Get the current size of the collection.
     *
     * @return the size
     */
    @Override
    public int getSize()
    {
        return size;
    }

    /**
     * Is the current collection empty.
     *
     * @return true if collection is empty, false otherwise
     */
    @Override
    public boolean isEmpty()
    {
        return size == 0;
    }

    /** Clear the collection, empty out all of its contents. */
    @Override
    public void clear()
    {
        associations.clear();
        Arrays.fill( lefts, Const.NO_ENTRY );
        Arrays.fill( leftNexts, Const.NO_ENTRY );
        size = 0;
        if( countLefts )
        {
            Arrays.fill( leftCounts, 0 );
        }
    }

    /**
     * Check if two numbers are associated. Simple check against HashSetLong.
     *
     * @param left  left to check
     * @param right right to check
     * @return true if associated, false otherwise
     */
    public boolean isAssociated( int left, int right )
    {
        return associations.contains( NumberUtil.packLong( left, right ) );
    }

    /**
     * <p>
     * Method used for iterating through the rights for one left. Will return the next entry that contains our
     * desired right, requiring the previous entry, as well as the left. If we are just starting to iterate, the
     * <b>prevEntry</b> should be Const.NO_ENTRY.
     * </p>
     *
     * @param left
     * @param prevEntry
     * @return
     */
    public int getNextRightEntry( int left, int prevEntry )
    {
        if( prevEntry == Const.NO_ENTRY )
        {
            return lefts[ left ];
        }
        return leftNexts[ prevEntry ];
    }

    /**
     * Get the right integer in an entry of our collection.
     *
     * @param entry the entry of the association
     * @return the right integer in the association
     */
    public int getRight( int entry )
    {
        return NumberUtil.getRight( associations.get( entry ) );
    }

    /**
     * Get the left integer in an entry of our collection.
     *
     * @param entry the entry of the association
     * @return the left integer in the association
     */
    public int getLeft( int entry )
    {
        return NumberUtil.getLeft( associations.get( entry ) );
    }


    /**
     * For a given left, return the values of the right in the target array. If the target array
     * is null, then we will return an array that is completely full of associations. If the array
     * is larger than the number of associations (or if our growth strategy grows the array past that number),
     * then we will mark the end of the associations with the <b>mark</b> passed.
     *
     * @param left   the left that we would like to get the associations for
     * @param target the target array
     * @return the target array that contains all the associations for a left.
     */
    public int[] getAllRightAssociations( int left, int[] target, int mark )
    {
        if (countLefts)
        {
            int len = leftCounts[left];
            if (target == null || target.length<len)
            {
                target = intFactory.alloc( len );
            }
        }
        if( target == null )
        {
            target = intFactory.alloc( DEFAULT_REL_SIZE );
        }
        int entry = getNextRightEntry( left, Const.NO_ENTRY );
        int ct =0;
        if( countLefts ) //we know we have perfectly sized array
        {
            while( entry != Const.NO_ENTRY )
            {
                target[ ct++ ] = getRight( entry );
                entry = getNextRightEntry( left, entry );
            }
        }
        else   //must ensure capacity and add mark
        {
            while( entry != Const.NO_ENTRY )
            {
                intFactory.ensureArrayCapacity( target, ct + 1, growthStrategy );
                target[ ct++ ] = getRight( entry );
                entry = getNextRightEntry( left, entry );
            }
        }
        if( ct < target.length )
        {
            target[ ct ] = mark;
        }
        return target;
    }

    public boolean disassociate( int left, int right )
    {
        long val = NumberUtil.packLong( left, right );
        int entry = associations.getEntry( NumberUtil.packLong( left, right ) );
        //check existence, return false if doesnt
        if( entry == Const.NO_ENTRY )
        {
            return false;
        }
        associations.remove( val );
        size--;
        if (countLefts)
        {
            leftCounts[left]--;
        }

        //remove from linked list
        int next;
        int testEntry = lefts[ left ];
        if( testEntry == entry ) //removing first
        {
            next = leftNexts[testEntry];
            if (next!=Const.NO_ENTRY)
            //switch left to the next item, mark the next with Const.NO_ENTRY. The left will
            //correctly point to the correct next, if any
            {
                lefts[left] = next;
                leftNexts[testEntry] = Const.NO_ENTRY;
            }
            return true;
        }
        //not first entry, start cycling leftNexts array
        testEntry = leftNexts[testEntry];
        while (testEntry!=entry)
        {
            testEntry = leftNexts[testEntry];
        }
        next = leftNexts[testEntry];
        leftNexts[testEntry] = next;
        if (next!=Const.NO_ENTRY)
        {
            leftNexts[next] = Const.NO_ENTRY;
        }
        return true;
    }

    public int getCountForLeft( int left )
    {
        if( countLefts ) return leftCounts[ left ];
        int entry = getNextRightEntry( left, Const.NO_ENTRY );
        int ct = 0;
        while( entry != Const.NO_ENTRY )
        {
            ct++;
            entry = getNextRightEntry( left, entry );
        }
        return ct;
    }

    public OneToManyInt copy( OneToManyInt target )
    {
        return null;
    }
}