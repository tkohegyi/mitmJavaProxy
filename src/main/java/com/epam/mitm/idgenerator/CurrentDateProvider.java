package com.epam.mitm.idgenerator;
/*==========================================================================
Copyright since 2013, EPAM Systems
===========================================================================*/

import java.util.Date;

/**
 * Class that can provide the current date.
 *
 * @author Marton_Sereg, Tamas Kohegyi
 */
public class CurrentDateProvider {

    public Date getCurrentDate() {
        return new Date();
    }
}

