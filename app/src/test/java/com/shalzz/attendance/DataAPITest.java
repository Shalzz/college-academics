package com.shalzz.attendance;

import com.shalzz.attendance.data.remote.DataAPI;

import org.junit.Test;
import static junit.framework.Assert.assertEquals;

public class DataAPITest {

    @Test
    public void ApiEndpointIsCorrect() {
        assertEquals(DataAPI.ENDPOINT, "https://academics.8bitlabs.in/api/v1/");
    }
}