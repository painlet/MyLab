package com.notpanda.mylab

import android.net.Uri
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }


    @Test
    fun `test uri`(){
        //val uriStr ="http://localhost/path?a=1"
        val uriStr ="http://localhost/path?a=1"
        val uri = Uri.parse(uriStr)
        System.out.println(uri.path)
        System.out.println(uri.queryParameterNames)
    }
}
