package com.scanner.cryptoserver.exchange.proton

import com.scanner.cryptoserver.util.UrlReaderImpl
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner


@RunWith(SpringRunner::class)
//Here, we load only the components needed - this prevents a full Spring Boot test from running, as only certain components are needed.
//For example, startup initialization threads are not needed, etc.
@ContextConfiguration(classes = [UrlReaderImpl::class, ProtonApiServiceImpl::class])
@WebMvcTest
internal class ProtonApiServiceImplIntegTest {
    @Autowired
    lateinit var service: ProtonApiServiceImpl

    @Test
    fun `test that the service method 'getInfo()' returns valid info`() {
        val info = service.getInfo()
        println(info)
    }
}
