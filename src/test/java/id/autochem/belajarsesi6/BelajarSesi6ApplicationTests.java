package id.autochem.belajarsesi6;

import id.autochem.belajarsesi6.support.FixedClockConfiguration;
import id.autochem.belajarsesi6.support.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class})
class BelajarSesi6ApplicationTests {

    @Test
    void contextLoads() {
    }

}
