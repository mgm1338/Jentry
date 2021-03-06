package store.col.storage.generic;

/**
 * Copyright 4/24/13
 * All rights reserved.
 * <p/>
 * User: Max Miller
 * Created: 4/24/13
 */
public interface ColStoreCharSequence
{
    /**
     * Get the value for the row desired.
     *
     * @param row the row in the column
     * @return the value
     */
    CharSequence getValue( int row );

    /**
     * Set a value for a particular row in the column.
     *
     * @param val value for the row
     * @param row the row index
     */
    void setValue( CharSequence val, int row );

}
