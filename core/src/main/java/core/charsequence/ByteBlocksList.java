package core.charsequence;

import core.array.GrowthStrategy;
import core.array.MasterSlaveSortInt;
import core.array.SwappableInt;
import core.array.factory.ArrayFactoryByte;
import core.array.factory.ArrayFactoryInt;
import core.util.comparator.ComparatorInt;
import core.util.comparator.Comparators;

/**
 * Copyright 5/16/13
 * All rights reserved.
 * <p/>
 * User: Max Miller
 * Created: 5/16/13
 * <p/>
 * A collection of {@link ByteBlock} lists that will return a handle for every inserted {@link ByteBlock} upon
 * insertion.
 */
public class ByteBlocksList
{
    /** Default size for a block of bytes, does not restrict the size of the block, just allocates the byte array */
    public static final int DEFAULT_BLOCK_SIZE = 64;

    protected static final int DEFAULT_FREE_LIST_SIZE = 2;


    /** Factory for allocating offsets and lengths */
    protected final ArrayFactoryInt intFactory;
    /** Factory for allocating bytes that blocks making up the {@link ByteBlock}s */
    protected final ArrayFactoryByte byteFactory;
    /** Growth strategy of the bytes, and the number of blocks */
    protected final GrowthStrategy growthStrategy;

    /** The actual byte data of the {@link ByteBlock}s, this collection will own the byte array */
    protected byte[] data;
    /** Parallel arrays that will describe the location and length of {@link ByteBlock}s in list */
    protected int[] offsets;
    protected int[] lengths;

    protected int[] shiftArray;


    /** Current number of blocks */
    protected int nextBlock = 0;
    /** Free list of vacated entries */
    protected int[] freeList;
    /** Number of items in the free list */
    protected int freeListPtr = 0;
    /** After a compact, we can use the free entries, this will denote how many we have left */
    protected int freeListLockPtr = -1;
    /** Now when we are using free list entries, we will use up to <i>freeListLockPtr</i> */
    protected int freeListUsePtr = 0;


    /** Pointer in the bytes array */
    protected int dataPtr = 0;

    /** Scratch array that will grow to necessary size as temporary block holder */
    private byte[] scratch = new byte[ DEFAULT_BLOCK_SIZE ];
    protected int[] positionHolder = new int[ 8 ];
    protected int[] lenScratch = new int[ 8 ];

    protected final ComparatorInt cmp = new Comparators.IntAsc();
    protected int size = 0;


    public ByteBlocksList( int numBlocks )
    {
        this( numBlocks, DEFAULT_BLOCK_SIZE );
    }

    public ByteBlocksList( int numBlocks, int blockSize )
    {
        this( numBlocks, blockSize, ArrayFactoryInt.defaultIntProvider, ArrayFactoryByte.defaultByteProvider,
              GrowthStrategy.doubleGrowth );
    }

    public ByteBlocksList( int numBlocks, int blockSize, ArrayFactoryInt intFactory,
                           ArrayFactoryByte byteFactory,
                           GrowthStrategy growthStrategy )
    {
        offsets = intFactory.alloc( numBlocks );
        lengths = intFactory.alloc( numBlocks );
        data = byteFactory.alloc( blockSize * numBlocks );
        freeList = intFactory.alloc( DEFAULT_FREE_LIST_SIZE );
        this.intFactory = intFactory;
        this.byteFactory = byteFactory;
        this.growthStrategy = growthStrategy;
        this.shiftArray = intFactory.alloc( DEFAULT_FREE_LIST_SIZE );
    }

    public int insert( CharSequence block )
    {
        int len = block.length();
        if( scratch.length < len ) scratch = new byte[ len ]; //dont have to save any data with this
        for( int i = 0; i < len; i++ )
        {
            scratch[ i ] = ( byte ) block.charAt( i );
        }
        return insert( scratch, 0, len );
    }

    public int insert( byte[] block, int offset, int length )
    {
        int offsetsLen = offsets.length;
        int dataLen = data.length;
        //check growth conditions, if we need to grow we do a compact
        if( freeListPtr + nextBlock == offsetsLen ) //growing offset length arrays
        {
            compact();
            growOffsetsLengths( offsetsLen + 1 );
        }
        if( dataPtr + length > dataLen )     //growing bytes
        {
            compact();
            growData( dataLen );
        }
        //entry can either be next block, or if we have compacted we will use freelist entries that were freed
        int entry;
        if( freeListUsePtr < freeListLockPtr )
        {
            entry = freeList[ freeListUsePtr++ ];
        }
        else
        {
            entry = nextBlock++;
        }
        offsets[ entry ] = dataPtr;
        lengths[ entry ] = length;
        System.arraycopy( block, offset, data, dataPtr, length );
        dataPtr += length;
        size++;
        return entry;
    }

    private void growData( int dataLen )
    {
        data = byteFactory.grow( data, dataLen, growthStrategy );
    }

    private void growOffsetsLengths( int offsetsLen )
    {
        offsets = intFactory.grow( offsets, offsetsLen, growthStrategy );
        lengths = intFactory.grow( lengths, offsetsLen, growthStrategy );
    }

    public void remove( int blockIdx )
    {
        lengths[ blockIdx ] = -lengths[ blockIdx ];
        freeList = intFactory.ensureArrayCapacity( freeList, freeListPtr + 1, growthStrategy );
        freeList[ freeListPtr++ ] = blockIdx;
        size--;
    }

    /**
     * <p>
     * Compacting will free the removed bytes in the data array, and allow entries to be re-used.
     * </p>
     * <p/>
     * <p>Because {@link ByteBlock}s can be of variable length, we cannot re-use their state until we have
     * completely cleared the old data. Compact will sort the offsets in ascending order, </p>
     */
    public void compact()
    {
        if( freeListPtr == 0 ) return; //nothing to compact

        int offsetLen = offsets.length;
        if( offsetLen > positionHolder.length )
        {
            positionHolder = new int[ offsetLen ];
        }
        for (int i=0; i<offsetLen; i++) //scratch stores the slots
        {
            positionHolder[i] = i;
        }
        MasterSlaveSortInt.sort( offsets, 0, size, cmp, 2, new SwappableInt( lengths ), //sort the offsets, keeping
                                 new SwappableInt( positionHolder ) );  //the positions and length in parallel order
        int totalSize = size + freeListPtr;

        //if we have items in the free list, we need to 'squish holes' and shift everything left
        if( freeListPtr > 0 )
        {

            int shift = 0; //rolling total of how much to squish left remaining elements
            for( int i = 0; i < totalSize; i++ )
            {
                if( lengths[ i ] < 0 ) //a negative length means item was removed, old length is absolute value of length
                {
                    shift += lengths[ i ];
                }
                else
                {
                    if( shift != 0 ) //we need to shift this item left in the array
                    {
                        System.arraycopy( data, offsets[ i ], data, offsets[ i ] + shift, lengths[ i ] );
                        offsets[ i ] += shift;
                    }
                }
            }
            dataPtr+=shift; //move the data pointer the total shift
            MasterSlaveSortInt.sort( positionHolder, 0, size, cmp, 2,
                                     new SwappableInt( lengths ), new SwappableInt( offsets ) );
        }
        if( freeListUsePtr > 0 )    //shifts all un-used free list items to zero
        {
            int numFreeListLeft = freeListPtr - freeListUsePtr;
            System.arraycopy( freeList, freeListUsePtr, freeList, 0, numFreeListLeft );
            freeListPtr = numFreeListLeft;
        }
        freeListLockPtr = freeListPtr;
        freeListUsePtr = 0;
    }

    public int getSize()
    {
        return size;
    }


    public CharSequence getByteBlock( int idx )
    {
        int len = lengths[ idx ];
        if( len < 0 ) return null;
        byte[] target = new byte[ len ];
        System.arraycopy( data, offsets[ idx ], target, 0, len );
        return new CharSequenceBytes( target );
    }

}
