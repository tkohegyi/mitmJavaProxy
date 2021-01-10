package org.rockhill.mitm.util.idgenerator;

import java.util.Date;

/**
 * Class that can provide the current date.
 *
 * @author Tamas Kohegyi
 */
public class CurrentDateProvider {

    public Date getCurrentDate() {
        return new Date();
    }
}

