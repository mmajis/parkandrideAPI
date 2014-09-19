package fi.hsl.parkandride.core.outbound;

import java.util.List;

import fi.hsl.parkandride.core.domain.Facility;
import fi.hsl.parkandride.core.domain.FacilitySearch;

public interface FacilityRepository {

    long insertFacility(Facility facility);

    void updateFacility(long facilityId, Facility facility);

    void updateFacility(long facilityId, Facility newFacility, Facility oldFacility);

    Facility getFacility(long id);

    Facility getFacilityForUpdate(long id);

    List<Facility> findFacilities(FacilitySearch search);

}
