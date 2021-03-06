package collections.generic;

/**
 * User: Max Miller
 * Date: 1/14/13
 * <p/>
 * Similar to the JDK Collection interface, this will describe the non-type specific methods available to
 * every collection. These methods are limited, with the primitive-type based nature of the collections,
 * see {@link Collection_KeyTypeName_} and generated classes for type-specific interfaces.
 */
public interface Collection
{
    /**
     * Get the current size of the collection.
     *
     * @return the size
     */
    int getSize();

    /**
     * Is the current collection empty.
     *
     * @return true if collection is empty, false otherwise
     */
    boolean isEmpty();

    /** Clear the collection, empty out all of its contents. */
    void clear();

}
