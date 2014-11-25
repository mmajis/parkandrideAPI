package fi.hsl.parkandride.back;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import fi.hsl.parkandride.back.sql.QContact;
import fi.hsl.parkandride.back.sql.QService;
import fi.hsl.parkandride.core.back.ContactRepository;
import fi.hsl.parkandride.core.back.ServiceRepository;
import fi.hsl.parkandride.core.domain.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TestConfiguration.class)
public class ServiceDaoTest {

    @Inject
    TestHelper testHelper;

    @Inject
    ServiceRepository serviceDao;

    @Test
    public void get_by_id() {
        Service service = serviceDao.getService(1);
        assertThat(service.id).isEqualTo(1l);
        assertThat(service.name).isEqualTo(new MultilingualString("Hissi", "Elevator", "Elevator"));
    }

    @Test
    public void find_all_defaults() {
        SearchResults<Service> results = serviceDao.findServices(new ServiceSearch());
        assertThat(results.get(0).name.fi).isEqualTo("Autopesu");
        assertThat(results.get(1).name.fi).isEqualTo("Esteetön WC");
    }

    @Test
    public void find_all_sort_by_name_en() {
        ServiceSearch search = new ServiceSearch();
        search.sort = new Sort("name.en");
        SearchResults<Service> results = serviceDao.findServices(search);
        assertThat(results.get(0).name.en).isEqualTo("Car Wash");
        assertThat(results.get(1).name.en).isEqualTo("Covered");
    }

}