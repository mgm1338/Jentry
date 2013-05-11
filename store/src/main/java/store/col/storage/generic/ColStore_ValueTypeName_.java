package store.col.storage.generic;

import core.stub.*;
import store.col.storage.generic.*;

/**
 * Copyright 4/24/13
 * All rights reserved.
 * <p/>
 * User: Max Miller
 * Created: 4/24/13
 */
public interface ColStore_ValueTypeName_ extends TypedColumnStorage
{
    _val_ getValue(int row);

    void setValue(_val_ val, int row);

}